package com.rainbow.spiders;

import com.rainbow.common.DataFrame;
import com.rainbow.config.SpiderConfig;
import com.rainbow.tool.StatementEngine;
import edu.uci.ics.crawler4j.url.URLCanonicalizer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuming on 2017/10/17.
 */
public class HtmlFieldParser extends AbstractFieldParser {

    private static final Logger logger = LoggerFactory.getLogger(HtmlFieldParser.class);

    public HtmlFieldParser(StatementEngine engine, Map<String, String> insideVarMap, ConcurrentMap<String, String> parentVarMap, Set<String> filterSet) {
        super(engine, insideVarMap, parentVarMap, filterSet);
    }

    @Override
    public DataFrame parseFields(WebURL webURL, String data, SpiderConfig.Page page, Set<WebURL> outputUrls) {
        Document document = Jsoup.parse(data);

        // 提取页面中内嵌的JS代码，存入到页面级变量中
        String js = "";
        Elements elements = document.getElementsByTag("script");
        for (Element element : elements) {
            js += element.html();
        }
        insideVarMap.put("js", js);

        // 缓存当前页面的满足css选择器的URL，在shouldVisit中使用
        String filters = page.outputUrlFilter;
        for (String filter : filters.split(",")) {
            if (filter.trim().startsWith("!")) {
                // 元素选择器过滤
                cacheCssSelectorUrl(filter.trim().substring(1), document);
            }
        }

        initUrlAndPropsToInsideVarMap(webURL.getURL(), page.props);

        return parseHtmlFields(page, document, webURL, outputUrls);
    }

    @Override
    public <T> Set<String> getCssUrls(T t, String selector) {
        Set<String> set = new HashSet<>();

        Document document = (Document) t;

        Elements baseE = document.getElementsByTag("base");
        String baseUrl = null;
        if (!baseE.isEmpty()) {
            baseUrl = baseE.attr("href");
        } else {
            baseUrl = insideVarMap.get("url");
        }

        Elements selects = document.select(selector);

        for (Element select : selects) {
            if (select.hasAttr("href")) {
                set.add(URLCanonicalizer.getCanonicalURL(select.attr("href"), baseUrl));
            } else if (select.hasAttr("src")) {
                set.add(URLCanonicalizer.getCanonicalURL(select.attr("src"), baseUrl));
            } else if (select.hasAttr("hrefs")) {
                set.add(URLCanonicalizer.getCanonicalURL(select.attr("hrefs"), baseUrl));
            }
        }

        if (set.isEmpty()) {
            logger.warn("get nothing for custom url field: {}", selector);
        }

        return set;
    }

    private DataFrame parseHtmlFields(SpiderConfig.Page page, Document document, WebURL webURL, Set<WebURL> outputUrls) {
        DataFrame repeatedDF = new DataFrame();
        DataFrame noRepeatedDF = new DataFrame();
        for (SpiderConfig.Page.Field field : page.fields) {
            String name = field.name;
            String selector = field.selector;
            boolean repeated = field.repeated;
            String transfer = field.transfer;
            boolean isVar = field.isVar;

            if (StringUtils.isNotBlank(selector)) {
                Elements elements = document.select(selector);
                if (elements.isEmpty()) {
                    logger.warn("current url: {}", insideVarMap.get("url"));
                    logger.warn("get nothing from page for: {}, skipped!", selector);
                    break;
                }

                if (repeated) {
                    if (repeatedDF.rows() > 0 && repeatedDF.rows() != elements.size()) {
                        logger.error("all repeated field values not equal!");
                        break;
                    }

                    int count = 0;
                    for (Element element : elements) {
                        String value = element.text();

                        insideVarMap.put(name, value);
                        String newValue = transfer(value, transfer, webURL);
                        repeatedDF.insertColumn(count++, name, newValue);

                        insideVarMap.put(name, newValue);
                    }

                } else {
                    String value = elements.get(0).text();

                    insideVarMap.put(name, value);
                    String newValue = transfer(value, transfer, webURL);
                    noRepeatedDF.addColumn(name, newValue);

                    insideVarMap.put(name, newValue);

                    if (isVar) {
                        String baseKey = webURL.getCrawlId() + "_" + webURL.getDocid();
                        parentVarMap.put(baseKey + "_" + name, newValue);
                    }
                }
            } else {
                String newValue = transfer("", transfer, webURL);
                insideVarMap.put(name, newValue);

                noRepeatedDF.addColumn(name, newValue);

                if (isVar) {
                    String baseKey = webURL.getCrawlId() + "_" + webURL.getDocid();
                    parentVarMap.put(baseKey + "_" + name, newValue);
                }
            }
        }

        // 添加自定义URL
        addCustomUrls(page, document, webURL, outputUrls);

        return DataFrame.merge(noRepeatedDF, repeatedDF);
    }

    private void cacheCssSelectorUrl(String cssSelector, Document document) {
        filterSet.addAll(getCssUrls(document, cssSelector));

        logger.debug("set: {}", filterSet);
    }

}
