package com.rainbow.common;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.authentication.AuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.BasicAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.FormAuthInfo;
import edu.uci.ics.crawler4j.crawler.authentication.NtAuthInfo;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuming on 2017/10/13.
 */
public class ProxyHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(ProxyHttpClient.class);

    private ThreadLocal<HttpClientBuilder> httpClientBuilderTL = new ThreadLocal<>();
    private ThreadLocal<CloseableHttpClient> httpClientTL = new ThreadLocal<>();
    private ThreadLocal<String> lastProxyHostTL = new ThreadLocal<>();

    private CrawlConfig config = null;
    private PoolingHttpClientConnectionManager connectionManager = null;

    public ProxyHttpClient(CrawlConfig config, PoolingHttpClientConnectionManager manager) {
        this.config = config;
        this.connectionManager = manager;
    }

    public String get(String url, String referer) throws IOException {
        int count = 0;
        IOException exception = null;
        while (count++ <= 5) {
            try {
                buildHttpClient();
                return HttpUtil.get(httpClientTL.get(), url, referer);
            } catch (IOException e) {
                logger.warn("current try times: {}", count);
                exception = e;
            }
        }

        throw exception;
    }

    public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
        int count = 0;
        IOException exception = null;
        while (count++ <= 5) {
            try {
                buildHttpClient();
                return httpClientTL.get().execute(request);
            } catch (IOException e) {
                logger.warn("current try times: {}", count);
                exception = e;
            }
        }

        throw exception;
    }

    private void buildHttpClient() throws IOException {
        HttpClientBuilder httpClientBuilder = httpClientBuilderTL.get();
        if (httpClientBuilder == null) {
            RequestConfig requestConfig = RequestConfig.custom()
                    .setExpectContinueEnabled(false)
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setRedirectsEnabled(false)
                    .setSocketTimeout(config.getSocketTimeout())
                    .setConnectTimeout(config.getConnectionTimeout())
                    .setConnectionRequestTimeout(config.getConnectionTimeout())
                    .build();

            SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(config.getSocketTimeout()).build();

            httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            httpClientBuilder.setUserAgent(config.getUserAgentString());
            httpClientBuilder.setDefaultHeaders(config.getDefaultHeaders());
            httpClientBuilder.setConnectionManagerShared(true);
            httpClientBuilder.setDefaultSocketConfig(socketConfig);

            if (connectionManager != null) {
                httpClientBuilder.setConnectionManager(connectionManager);
            }

            httpClientBuilderTL.set(httpClientBuilder);
        }

        if (StringUtils.isNotBlank(config.getProxyHost())) {
            // 设置了代理
            String str = HttpUtil.get(config.getProxyHost());
            String[] split = str.split(":", -1);

            if (split.length == 2) {
                String proxyHost = split[0].trim();
                String proxyPort = split[1].trim();

                String lastProxyHost = lastProxyHostTL.get();
                if (!proxyHost.equals(lastProxyHost)) {
                    CloseableHttpClient httpClient = httpClientTL.get();
                    if (httpClient != null) {
                        try {
                            httpClient.close();
                            httpClientTL.remove();
                        } catch (IOException e) {
                            logger.warn("close httpClient error: ", e);
                        }
                    }

                    lastProxyHostTL.set(proxyHost);

                    if (config.getProxyUsername() != null) {
                        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(
                                new AuthScope(config.getProxyHost(), config.getProxyPort()),
                                new UsernamePasswordCredentials(config.getProxyUsername(),
                                        config.getProxyPassword()));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
                    httpClientBuilder.setProxy(proxy);
                    logger.debug("Working through Proxy: {}, Port: {}", proxyHost, proxyPort);

                    httpClient = httpClientBuilder.build();
                    httpClientTL.set(httpClient);

                    if ((config.getAuthInfos() != null) && !config.getAuthInfos().isEmpty()) {
                        doAuthetication(config.getAuthInfos());
                    }
                }
            } else {
                logger.error("get proxy host failed! get str: {}", str);
            }
        } else {
            CloseableHttpClient httpClient = httpClientTL.get();
            if (httpClient == null) {
                httpClient = httpClientBuilder.build();
                httpClientTL.set(httpClient);

                if ((config.getAuthInfos() != null) && !config.getAuthInfos().isEmpty()) {
                    doAuthetication(config.getAuthInfos());
                }
            }
        }
    }

    public void close() throws IOException {
        CloseableHttpClient httpClient = httpClientTL.get();
        if (httpClient != null) {
            httpClient.close();
        }

        httpClientTL.remove();
        httpClientBuilderTL.remove();
        lastProxyHostTL.remove();
    }

    private void doAuthetication(List<AuthInfo> authInfos) throws IOException {
        for (AuthInfo authInfo : authInfos) {
            if (authInfo.getAuthenticationType() ==
                    AuthInfo.AuthenticationType.BASIC_AUTHENTICATION) {
                doBasicLogin((BasicAuthInfo) authInfo);
            } else if (authInfo.getAuthenticationType() ==
                    AuthInfo.AuthenticationType.NT_AUTHENTICATION) {
                doNtLogin((NtAuthInfo) authInfo);
            } else {
                doFormLogin((FormAuthInfo) authInfo);
            }
        }
    }

    /**
     * BASIC authentication<br/>
     * Official Example: https://hc.apache
     * .org/httpcomponents-client-ga/httpclient/examples/org/apache/http/examples
     * /client/ClientAuthentication.java
     * */
    private void doBasicLogin(BasicAuthInfo authInfo) throws IOException {
        logger.info("BASIC authentication for: " + authInfo.getLoginTarget());
        HttpHost targetHost =
                new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                new UsernamePasswordCredentials(authInfo.getUsername(),
                        authInfo.getPassword()));

        CloseableHttpClient closeableHttpClient = httpClientTL.get();
        if (closeableHttpClient != null) {
            closeableHttpClient.close();
            httpClientTL.remove();
        }
        httpClientTL.set(HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build());
    }

    /**
     * Do NT auth for Microsoft AD sites.
     */
    private void doNtLogin(NtAuthInfo authInfo) throws IOException {
        logger.info("NT authentication for: " + authInfo.getLoginTarget());
        HttpHost targetHost =
                new HttpHost(authInfo.getHost(), authInfo.getPort(), authInfo.getProtocol());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        try {
            credsProvider.setCredentials(
                    new AuthScope(targetHost.getHostName(), targetHost.getPort()),
                    new NTCredentials(authInfo.getUsername(), authInfo.getPassword(),
                            InetAddress.getLocalHost().getHostName(), authInfo.getDomain()));
        } catch (UnknownHostException e) {
            logger.error("Error creating NT credentials", e);
        }

        CloseableHttpClient closeableHttpClient = httpClientTL.get();
        if (closeableHttpClient != null) {
            closeableHttpClient.close();
            httpClientTL.remove();
        }
        httpClientTL.set(HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build());
    }

    /**
     * FORM authentication<br/>
     * Official Example:
     *  https://hc.apache.org/httpcomponents-client-ga/httpclient/examples/org/apache/http
     *  /examples/client/ClientFormLogin.java
     */
    private void doFormLogin(FormAuthInfo authInfo) {
        logger.info("FORM authentication for: " + authInfo.getLoginTarget());
        String fullUri =
                authInfo.getProtocol() + "://" + authInfo.getHost() + ":" + authInfo.getPort() +
                        authInfo.getLoginTarget();
        HttpPost httpPost = new HttpPost(fullUri);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(
                new BasicNameValuePair(authInfo.getUsernameFormStr(), authInfo.getUsername()));
        formParams.add(
                new BasicNameValuePair(authInfo.getPasswordFormStr(), authInfo.getPassword()));

        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, "UTF-8");
            httpPost.setEntity(entity);
            httpClientTL.get().execute(httpPost);
            logger.debug("Successfully Logged in with user: " + authInfo.getUsername() + " to: " +
                    authInfo.getHost());
        } catch (UnsupportedEncodingException e) {
            logger.error("Encountered a non supported encoding while trying to login to: " +
                    authInfo.getHost(), e);
        } catch (ClientProtocolException e) {
            logger.error("While trying to login to: " + authInfo.getHost() +
                    " - Client protocol not supported", e);
        } catch (IOException e) {
            logger.error(
                    "While trying to login to: " + authInfo.getHost() + " - Error making request", e);
        }
    }
}
