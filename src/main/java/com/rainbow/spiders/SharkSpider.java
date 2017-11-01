package com.rainbow.spiders;

import com.rainbow.common.DataFrame;
import com.rainbow.common.ProxyHttpClient;
import com.rainbow.config.SpiderConfig;
import com.rainbow.controller.ControllerContext;
import com.rainbow.controller.SpiderController;
import com.rainbow.storage.StorageManager;
import com.rainbow.tool.StatementEngine;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.parser.ParseData;
import edu.uci.ics.crawler4j.parser.TextParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by xuming on 2017/9/25.
 */
public class SharkSpider extends WebCrawler {

    private static final Logger logger = LoggerFactory.getLogger(SharkSpider.class);
    private ControllerContext context = null;
    private SpiderConfig.Page spiderPage = null;

    private StorageManager manager = null;
    private Set<String> set = new HashSet<>();
    private Map<String, String> insideVarMap = new HashMap<>();

    private StatementEngine engine = new StatementEngine();

    @Override
    protected void onUnhandledException(WebURL webUrl, Throwable e) {
        logger.error("has unhandled exception: ", e);
    }

    @Override
    public void onStart() {
        SpiderController controller = (SpiderController) getMyController();
        context = controller.getContext();
        engine.init(new ProxyHttpClient(controller.getConfig(), null));
        try {
            manager = StorageManager.create(context.getStorageConfigs());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onBeforeExit() {
        try {
            engine.destroy();
            // 关闭PageFetcher中跟该线程相关的代理HTTP资源
            ProxyHttpClient proxyClient = getMyController().getPageFetcher().proxyClient;
            if (proxyClient != null) {
                try {
                    proxyClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            manager.close();
        } catch (IOException e) {
            logger.warn("error when close storage!", e);
        }
    }

    @Override
    public void visit(Page page) {
        WebURL webURL = page.getWebURL();

        set.clear();
        insideVarMap.clear();

        this.spiderPage = findSpiderPage(webURL);
        if (spiderPage != null) {
            ParseData parseData = page.getParseData();

            AbstractFieldParser parser = null;
            String content = null;
            if (parseData instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) parseData;
                content = htmlParseData.getHtml();
                String title = htmlParseData.getTitle();

                if (StringUtils.isBlank(title) && !content.contains("<body>")) {
                    parser = new JsonFieldParser(engine, insideVarMap, context.getVarMap(), set);
                } else {
                    parser = new HtmlFieldParser(engine, insideVarMap, context.getVarMap(), set);
                }

            } else if (parseData instanceof TextParseData) {
                TextParseData textParseData = (TextParseData) parseData;
                content = textParseData.getTextContent();

                parser = new JsonFieldParser(engine, insideVarMap, context.getVarMap(), set);
            }

            try {
                DataFrame df = parser.parseFields(webURL, content, spiderPage, parseData.getOutgoingUrls());
                if (spiderPage.isLastPage) {
                    save(webURL, df);
                }

            } catch (Exception e) {
                logger.warn("parse field error from page: {}", webURL.getURL());
                logger.error("exception: ", e);
            }
        } else {
            String url = webURL.getURL();
            logger.error("cannot found spider for url: {}", url);
        }

    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String redirectedToUrl = referringPage.getRedirectedToUrl();
        if (redirectedToUrl != null && redirectedToUrl.equals(url.getURL())) {
            logger.debug("redirect to url: {}", url.getURL());
            //上一页的重定向页面，直接返回true
            return true;
        }
        if (spiderPage != null && !spiderPage.isLastPage) {
            String filters = spiderPage.outputUrlFilter;
            if (StringUtils.isNotBlank(filters)) {
                for (String filter : filters.split(";")) {
                    if (filter.trim().startsWith("!")) {
                        // 元素选择器过滤
                        if (set.contains(url.getURL())) {
                            logger.debug("url: {} matched, depth: {}", url.getURL(), url.getDepth());
                            return true;
                        }
                    } else {
                        // 正则表达式过滤
                        Pattern pattern = Pattern.compile(filter.trim());

                        if (pattern.matcher(url.getURL()).matches()) {
                            logger.debug("url: {} matched, depth: {}", url.getURL(), url.getDepth());
                            return true;
                        }
                    }
                }
            } else {
                logger.debug("url: {} matched, depth: {}", url.getURL(), url.getDepth());
                return true;
            }
        }

        return false;
    }

    private void save(WebURL webURL, DataFrame df) {
        String primaryKey = "";
        if (StringUtils.isNotBlank(spiderPage.primaryKey)) {
            primaryKey = engine.transformExpression(spiderPage.primaryKey, insideVarMap).get(0);
        }

        String rowKey = webURL.getCrawlId() + "_" + primaryKey;
        try {
            manager.save(df, rowKey);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SpiderConfig.Page findSpiderPage(WebURL url) {
        Map<String, SpiderConfig> spiderConfigMap = context.getSpiderConfigMap();
        String spiderId = url.getCrawlId();
        if (spiderConfigMap.containsKey(spiderId)) {
            SpiderConfig spiderConfig = spiderConfigMap.get(spiderId);
            if (url.getDepth() > spiderConfig.pages.size() - 1) {
                logger.error("page chain define error!");
            } else {
                SpiderConfig.Page page = spiderConfig.pages.get(url.getDepth());
                if (url.getDepth() == spiderConfig.pages.size() - 1) {
                    page.isLastPage = true;
                }
                return page;
            }
        } else {
            logger.error("cannot find spider id from WebURL!");
        }

        return null;
    }


}
