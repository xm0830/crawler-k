package com.rainbow.tool;

import com.rainbow.common.ProxyHttpClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xuming on 2017/9/29.
 */
public class StatementEngine {

    private static final Logger logger = LoggerFactory.getLogger(StatementEngine.class);

    private final String expressionStartStr = "${";
    private final String expressionEndStr = "}";
    private final String methodParamStartStr = "(";
    private final String methodParamEndStart = ")";
    private final String methodParamSep = "<>";

    private MethodInside methodInside = new MethodInside();

    public void init(ProxyHttpClient client) {
        methodInside.init(client);
    }

    public void destroy() {
        methodInside.destroy();
    }

    public List<String> transformExpression(String str, Map<String, String> vars) {
        if (StringUtils.isBlank(str)) return new ArrayList<>();

        str = StringUtils.deleteWhitespace(str);
        str = str.replaceAll(",", methodParamSep);

        // 替换变量
        StatementToken statementToken = parse(str, 0, true);
        while (statementToken.varDesc != null) {
            str = replaceVar(str, statementToken.varDesc, null, null, vars);

            statementToken = parse(str, 0, true);
        }

//        logger.debug("str: {}", str);

        // 替换函数
        return transformAllMethods(str);
    }

    public List<String> transformExpression(String str, String baseKey, Map<String, String> vars) {
        List<String> rt = new ArrayList<>();

        if (StringUtils.isBlank(str)) return rt;

        str = StringUtils.deleteWhitespace(str);
        str = str.replaceAll(",", methodParamSep);

        // 替换变量
        StatementToken statementToken = parse(str, 0, true);
        while (statementToken.varDesc != null) {
            str = replaceVar(str, statementToken.varDesc, baseKey, vars, null);

            statementToken = parse(str, 0, true);
        }

//        logger.debug("str: {}", str);

        // 替换函数
        return transformAllMethods(str);
    }

    public List<String> transformExpression(String str, String baseKey, Map<String, String> vars1, Map<String, String> vars2) {
        List<String> rt = new ArrayList<>();

        if (StringUtils.isBlank(str)) return rt;

        str = StringUtils.deleteWhitespace(str);
        str = str.replaceAll(",", methodParamSep);

        // 替换变量
        StatementToken statementToken = parse(str, 0, true);
        while (statementToken.varDesc != null) {
            str = replaceVar(str, statementToken.varDesc, baseKey, vars1, vars2);

            statementToken = parse(str, 0, true);
        }

//        logger.debug("str: {}", str);

        // 替换函数
        return transformAllMethods(str);
    }

    private List<String> transformAllMethods(String original) {
        List<String> ret = new ArrayList<>();

        StatementToken statementToken = parse(original, 0, false);
        if (statementToken.methodDesc != null) {
            List<String> list = replaceMethod(original, statementToken.methodDesc);
            for (String s : list) {
                statementToken = parse(s, 0, false);
                if (statementToken.methodDesc != null) {
                    ret.addAll(transformAllMethods(s));
                } else {
                    ret.add(s);
                }
            }
        } else {
            ret.add(original);
        }

        return ret;
    }

    private StatementToken parse(String original, int fromIndex, boolean isVar) {
        StatementToken statementToken = new StatementToken();

        int start = findExpressionStart(original, fromIndex);
        if (start == -1) return statementToken;

        int end = findExpressionEnd(original, start);

        if (end != -1) {
            String token = original.substring(start, end);
            int paramStart = token.indexOf(methodParamStartStr);
            int paramEnd = token.lastIndexOf(methodParamEndStart);

            if (paramStart != -1 && paramEnd != -1) {
                // 是函数
                if (isVar) {
                    return parse(original, end + 1, true);
                } else {
                    MethodDesc desc = new MethodDesc();
                    desc.methodName = token.substring(0, paramStart);
                    desc.methodParams = token.substring(paramStart + 1, paramEnd).split(methodParamSep, -1);

                    statementToken.methodDesc = desc;
                }
            } else {
                // 是变量
                if (isVar) {
                    VarDesc desc = new VarDesc();
                    desc.varName = token;

                    statementToken.varDesc = desc;
                } else {
                    return parse(original, end + 1, false);
                }
            }

        } else {
            throw new RuntimeException("${ not closed for: " + original);
        }


        return statementToken;
    }

    private int findExpressionStart(String original, int fromIndex) {
        int start = original.indexOf(expressionStartStr, fromIndex);
        while (start != -1) {
            start += expressionStartStr.length();

            int end = original.indexOf(expressionEndStr, start);
            int nextStart = original.indexOf(expressionStartStr, start);

            if (end == -1) {
                throw new RuntimeException("${ not closed for: " + original);
            }

            if (nextStart == -1 || nextStart > end) {
                return start;
            } else {
                start = nextStart;
            }
        }

        return -1;
    }

    private int findExpressionEnd(String original, int fromIndex) {
        int end = original.indexOf(expressionEndStr, fromIndex);
        while (end != -1) {
            int nextStart = original.indexOf("{", fromIndex);

            if (nextStart == -1 || nextStart > end) {
                return end;
            } else {
                end = original.indexOf(expressionEndStr, end + 1);
                fromIndex = nextStart + 1;
            }
        }

        return -1;
    }


    private String replaceVar(String original, VarDesc desc, String baseKey, Map<String, String> map1, Map<String, String> map2) {

        int flag = -1; // 1使用页面级变量，2使用父页面级变量，3既使用页面级变量又使用父页面级变量
        if (map2 != null) {
            flag = 1;
        }

        if (map1 != null) {
            if (flag == 1) {
                flag = 3;
            } else {
                flag = 2;
            }
        }

        String key;
        switch (flag) {
            case 1:
                key = desc.varName;
                if (map2.containsKey(key)) {
                    String varValue = map2.get(key);
                    String holder = "${" + desc.varName + "}";
                    while (original.contains(holder)) {
                        original = original.replace(holder, varValue);
                    }
                } else {
                    throw new RuntimeException(String.format("there is no var: %s define from current page!", key));
                }
                break;
            case 2:
                key = baseKey + "_" + desc.varName;
                if (map1.containsKey(key)) {
                    String varValue = map1.get(key);
                    String holder = "${" + desc.varName + "}";
                    while (original.contains(holder)) {
                        original = original.replace(holder, varValue);
                    }
                } else {
                    throw new RuntimeException(String.format("there is no var: %s define from parent page!", key));
                }
                break;
            case 3:
                key = desc.varName;
                String varValue = null;
                if (map2.containsKey(key)) {
                    varValue = map2.get(key);
                } else {
                    key = baseKey + "_" + desc.varName;
                    if (map1.containsKey(key)) {
                        varValue = map1.get(key);
                    }
                }

                if (varValue != null) {
                    String holder = "${" + desc.varName + "}";
                    while (original.contains(holder)) {
                        original = original.replace(holder, varValue);
                    }
                } else {
                    throw new RuntimeException(String.format("there is no var: %s define from current or parent page!", key));
                }
                break;

            case -1:
                throw new RuntimeException("current page map and parent page map is empty!");
        }

        return original;
    }

    private List<String> replaceMethod(String original, MethodDesc desc) {
        List<String> rt = new ArrayList<>();

        List listReturnValues = null;
        MethodDesc listReturnMethodDesc = null;
        Class<MethodInside> clazz = MethodInside.class;

        Method method = null;
        Object v = null;

        try {
            int size = desc.methodParams.length;

            if (size == 1 && desc.methodParams[0].toString().isEmpty()) {
                method = clazz.getDeclaredMethod(desc.methodName);
                v = method.invoke(methodInside);
            } else {
                Class[] paramClasses = new Class[size];
                for (int i = 0; i < size; i++) {
                    paramClasses[i] = String.class;
                }

                method = clazz.getDeclaredMethod(desc.methodName, paramClasses);
                v = method.invoke(methodInside, desc.methodParams);
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (v instanceof List) {
            listReturnValues = (List) v;
            listReturnMethodDesc = desc;
        } else if (v instanceof String) {
            String holder = "${" + desc.methodName + "(" + StringUtils.join(desc.methodParams, methodParamSep) + ")}";
            while (original.contains(holder)) {
                original = original.replace(holder, v.toString());
            }
        } else {
            throw new RuntimeException("only support List<String> or String return type!");
        }

        // 替换多返回值函数
        if (listReturnValues != null) {
            for (Object value : listReturnValues) {
                String holder = "${" + listReturnMethodDesc.methodName + "(" + StringUtils.join(listReturnMethodDesc.methodParams, methodParamSep) + ")}";
                rt.add(original.replace(holder, value.toString()));
            }
        } else {
            rt.add(original);
        }

        return rt;
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        String str = "http://www.baidu.com?a=${incr(1, 10, 1)}&b=${name}&c=${age}";
        Map<String, String> context = new HashMap<>();
        context.put("1_name", "haha");
        context.put("1_age", "2");

        StatementEngine engine = new StatementEngine();
        engine.init(null);

        List<String> list = engine.transformExpression(str, "1", context);
        System.out.println(list);

        str = "${extract(${url}, abdf(.*)dff(.*)ihh, -)}";
        Map<String, String> map = new HashMap<>();
        map.put("url", "abdfkkkkkkdffiuihh");
        List<String> ret = engine.transformExpression(str, map);
        System.out.println(ret);

        engine.destroy();
    }

}
