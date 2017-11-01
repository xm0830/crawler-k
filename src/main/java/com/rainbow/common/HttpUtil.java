package com.rainbow.common;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by xuming on 2017/10/13.
 */
public class HttpUtil {

    private static CloseableHttpClient httpClient;

    static {
        RequestConfig requestConfig = RequestConfig.custom()
                .setExpectContinueEnabled(false)
                .setCookieSpec(CookieSpecs.STANDARD)
                .setRedirectsEnabled(false)
                .build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(requestConfig);
        clientBuilder.setUserAgent("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");

        httpClient = clientBuilder.build();
    }

    public static String get(String url) {
        return get(httpClient, url, null);
    }

    public static String get(CloseableHttpClient httpClient, String url, String referer) {
        HttpUriRequest request = new HttpGet(url);
        if (StringUtils.isNotBlank(referer)) {
            request.setHeader("Referer", referer);
        }

        try {
            CloseableHttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
            httpClient = null;
        }
    }
}
