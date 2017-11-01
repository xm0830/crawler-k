package com.rainbow.spiders;

import com.rainbow.common.DataFrame;
import com.rainbow.config.SpiderConfig;
import com.rainbow.tool.StatementEngine;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuming on 2017/10/17.
 */
public abstract class AbstractFieldParser {

    private static final Logger logger = LoggerFactory.getLogger(Logger.class);

    protected StatementEngine engine = null;
    protected Map<String, String> insideVarMap;
    protected ConcurrentMap<String, String> parentVarMap;
    protected Set<String> filterSet;

    public AbstractFieldParser(StatementEngine engine, Map<String, String> insideVarMap, ConcurrentMap<String, String> parentVarMap, Set<String> filterSet) {
        this.engine = engine;
        this.insideVarMap = insideVarMap;
        this.parentVarMap = parentVarMap;
        this.filterSet = filterSet;
    }

    public abstract DataFrame parseFields(WebURL curURL, String data, SpiderConfig.Page page, Set<WebURL> outputUrls);

    public abstract <T> Set<String> getCssUrls(T t, String selector);

    protected String transfer(String oldValue, String transfer, WebURL webURL) {
        if (StringUtils.isNotBlank(transfer)) {
            String baseKey = webURL.getCrawlId() + "_" + webURL.getParentDocid();
            return engine.transformExpression(transfer, baseKey, parentVarMap, insideVarMap).get(0);
        } else {
            return oldValue;
        }
    }

    protected void initUrlAndPropsToInsideVarMap(String curUrl, List<SpiderConfig.Page.Prop> props) {
        insideVarMap.put("url", curUrl);
        for (SpiderConfig.Page.Prop prop : props) {
            if (StringUtils.isNotBlank(prop.name) && StringUtils.isNotBlank(prop.value)) {
                String newValue = engine.transformExpression(prop.value, insideVarMap).get(0);
                insideVarMap.put(prop.name, newValue);
            }
        }
    }

    protected <T> void addCustomUrls(SpiderConfig.Page page, T t, WebURL webURL, Set<WebURL> outputUrls) {
        if (!page.isLastPage && StringUtils.isNotBlank(page.customOutputUrls)) {
            outputUrls.clear();
            if (page.customOutputUrls.startsWith("!")) {
                addCssCustomUrls(t, page.customOutputUrls.substring(1), outputUrls);
            } else {
                addExpressionCustomUrls(webURL, page.customOutputUrls, outputUrls);
            }
        }
    }

    private void addExpressionCustomUrls(WebURL webURL, String expression, Set<WebURL> outputUrls) {
        if (StringUtils.isNotBlank(expression)) {

            String baseKey = webURL.getCrawlId() + "_" + webURL.getParentDocid();
            List<String> extraUrls = engine.transformExpression(expression, baseKey, parentVarMap, insideVarMap);
            for (String extraUrl : extraUrls) {
                logger.debug("add expression custom url: {}", extraUrl);

                WebURL extraWebUrl = new WebURL();
                extraWebUrl.setURL(extraUrl.replaceAll("&amp;", "&"));
                outputUrls.add(extraWebUrl);
            }
        }
    }

    private <T> void addCssCustomUrls(T t, String selector, Set<WebURL> outputUrls) {
        Set<String> urls = getCssUrls(t, selector);
        for (String url : urls) {
            logger.debug("add css custom url: {}", url);

            WebURL extraWebUrl = new WebURL();
            extraWebUrl.setURL(url.replaceAll("&amp;", "&"));
            outputUrls.add(extraWebUrl);
        }
    }
}
