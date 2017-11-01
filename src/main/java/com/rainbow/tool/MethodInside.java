package com.rainbow.tool;

import com.jayway.jsonpath.JsonPath;
import com.rainbow.common.JsonUtil;
import com.rainbow.common.ProxyHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xuming on 2017/10/11.
 */
public class MethodInside {
    private static final Logger logger = LoggerFactory.getLogger(MethodInside.class);

    private ProxyHttpClient client = null;

    public void init(ProxyHttpClient proxyHttpClient) {
        client = proxyHttpClient;
    }

    public List<String> incr(String from, String to, String step) {
        int intFrom = Integer.parseInt(from);
        int intTo = Integer.parseInt(to);
        int intStep = Integer.parseInt(step);

        List<String> ret = new ArrayList<>();

        for (int i = intFrom; i < intTo; i+=intStep) {
            ret.add(i + "");
        }

        return ret;
    }

    public String extract(String input, String regex) {
        return extract(input, regex, "_");
    }

    public String extract(String input, String regex, String sep) {

        if (input.startsWith("http") || input.startsWith("www")) {
            try {
                input = URLDecoder.decode(input, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(input);
        StringBuilder sb = new StringBuilder();
        if (matcher.matches()) {
            int groups = matcher.groupCount();
            for (int i = 1; i <= groups; i++) {
                String group = matcher.group(i);

                if (i == groups) {
                    sb.append(group);
                } else {
                    sb.append(group).append(sep);
                }
            }
        }

        return sb.toString();
    }

    public String requestForHtmlValue(String url, String selector) {
        logger.debug("requestForHtmlValue: {}", url);
        return requestForHtml(url, selector, "http://www.baidu.com").get(0);
    }

    public List<String> requestForHtmlValues(String url, String selector) {
        logger.debug("requestForHtmlValues: {}", url);
        return requestForHtml(url, selector, "http://www.baidu.com");
    }

    public String requestForJsonValue(String url, String selector) {
        logger.debug("requestForJsonValue: {}", url);
        return requestForJson(url, selector, "http://www.baidu.com").get(0);
    }

    public List<String> requestForJsonValues(String url, String selector) {
        logger.debug("requestForJsonValues: {}", url);
        return requestForJson(url, selector, "http://www.baidu.com");
    }

    public String getHtmlValue(String str, String selector) {
        return getHtmlValues(str, selector).get(0);
    }

    public List<String> getHtmlValues(String str, String selector) {
        Document document = Jsoup.parse(str);

        Elements elements = document.select(selector);

        List<String> ret = new ArrayList<>();

        for (Element element : elements) {
            ret.add(element.text());
        }

        return ret;
    }

    public String getJsonValue(String str, String selector) {
        return getJsonValues(str, selector).get(0);
    }

    public List<String> getJsonValues(String str, String selector) {
        Object obj = JsonPath.read(JsonUtil.trimJson(str), selector);

        List<String> ret = new ArrayList<>();

        if (obj instanceof List) {
            List elements = (List) obj;
            for (Object element : elements) {
                ret.add(element.toString());
            }
        } else {
            ret.add(obj.toString());
        }

        return ret;
    }

    public String time() {
        return System.currentTimeMillis() + "";
    }

    public String getVarFromJS(String js, String varName) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");

        InputStreamReader inputStreamReader = new InputStreamReader(MethodInside.class.getResourceAsStream("/json2.js"));
        try {
            engine.eval(inputStreamReader);
        } catch (ScriptException e) {
            logger.error("init js engine error!", e);
            return "";
        }

        if (!js.isEmpty()) {
            String[] strs = js.split("\\n", -1);
            for (String str : strs) {
                String trim = str.trim();
                if (trim.startsWith("var ") && trim.endsWith(";")) {
                    try {
                        engine.eval(str);
                    } catch (ScriptException e) {
//                        logger.debug("cannot execute js code: {}", js);
                    }
                }
            }
        }

        Object o = "";
        try {
            o = engine.eval("JSON.stringify("+ varName + ")");
        } catch (ScriptException e) {
            logger.error("transform JSON obj to string error!", e);
        }

        logger.info("getVarFromJS for var: {} is: {}", varName, o.toString());

        return o.toString();
    }

    private List<String> requestForHtml(String url, String selector, String referer) {
        String str = null;
        try {
            str = client.get(url, referer);
        } catch (IOException e) {
            logger.error("request error!", e);
        }

        List<String> ret = getHtmlValues(str, selector);

        logger.debug("requestForHtml result: {}", ret);

        return ret;
    }

    private List<String> requestForJson(String url, String selector, String referer) {
        String str = null;
        try {
            str = client.get(url, referer);
        } catch (IOException e) {
            logger.error("request error!", e);
        }

        List<String> ret = getJsonValues(str, selector);

        logger.debug("requestForJson result: {}", ret);

        return ret;
    }

    public void destroy() {
        if (client != null) {
            try {
                client.close();
                client = null;
            } catch (IOException e) {
                logger.warn("close resource failed!", e);
            }
        }
    }

}
