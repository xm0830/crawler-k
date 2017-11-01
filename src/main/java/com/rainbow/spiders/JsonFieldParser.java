package com.rainbow.spiders;

import com.jayway.jsonpath.JsonPath;
import com.rainbow.common.DataFrame;
import com.rainbow.common.JsonUtil;
import com.rainbow.config.SpiderConfig;
import com.rainbow.tool.StatementEngine;
import edu.uci.ics.crawler4j.url.WebURL;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by xuming on 2017/10/17.
 */
public class JsonFieldParser extends AbstractFieldParser {

    private static final Logger logger = LoggerFactory.getLogger(JsonFieldParser.class);

    public JsonFieldParser(StatementEngine engine, Map<String, String> insideVarMap, ConcurrentMap<String, String> parentVarMap, Set<String> filterSet) {
        super(engine, insideVarMap, parentVarMap, filterSet);
    }

    @Override
    public DataFrame parseFields(WebURL webURL, String data, SpiderConfig.Page page, Set<WebURL> outputUrls) {
        String str = JsonUtil.trimJson(data);

        initUrlAndPropsToInsideVarMap(webURL.getURL(), page.props);

        return parseJsonFields(page, str, webURL, outputUrls);
    }

    @Override
    public <T> Set<String> getCssUrls(T t, String selector) {
        Set<String> set = new HashSet<>();

        String str = (String) t;
        Object obj = JsonPath.read(str, selector);
        if (!(obj instanceof List)) {
            set.add(obj.toString());
        } else {
            List list = (List) obj;
            for (Object o : list) {
                set.add(o.toString());
            }
        }

        if (set.isEmpty()) {
            logger.warn("get nothing for custom url field: {}", selector);
        }

        return set;
    }

    private DataFrame parseJsonFields(SpiderConfig.Page page, String str, WebURL webURL, Set<WebURL> outputUrls) {
        DataFrame repeatedDF = new DataFrame();
        DataFrame noRepeatedDF = new DataFrame();
        for (SpiderConfig.Page.Field field : page.fields) {
            String name = field.name;
            String selector = field.selector;
            boolean repeated = field.repeated;
            String transfer = field.transfer;
            boolean isVar = field.isVar;

            if (StringUtils.isNotBlank(selector)) {
                if (repeated) {
                    List<String> elements = JsonPath.read(str, selector);

                    if (elements.isEmpty()) {
                        logger.warn("current url: {}", webURL.getURL());
                        logger.warn("get nothing from page for field: {}, selector: {}, skipped!", name, selector);
                        break;
                    }

                    if (repeatedDF.rows() > 0 && repeatedDF.rows() != elements.size()) {
                        logger.error("all repeated field values not equal to other repeated fields!");
                        break;
                    }

                    int count = 0;
                    for (String element : elements) {
                        insideVarMap.put(name, element);
                        String newValue = transfer(element, transfer, webURL);
                        repeatedDF.insertColumn(count++, name, newValue);

                        insideVarMap.put(name, newValue);
                    }
                } else {
                    String value = JsonPath.read(str, selector);

                    if (value == null) {
                        logger.warn("current url: {}", webURL.getURL());
                        logger.warn("get nothing from page for field: {}, selector: {}, skipped!", name, selector);
                        break;
                    }

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
        addCustomUrls(page, str, webURL, outputUrls);

        return DataFrame.merge(noRepeatedDF, repeatedDF);
    }

}
