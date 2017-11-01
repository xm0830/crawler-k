package com.rainbow.tool;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xuming on 2017/10/27.
 */
public class StatementEngineTest {
    static {
        System.setProperty("TEST_MODE", "true");
    }

    @Test
    public void testTransformExpression1() {
        String str = "aa${aa}${bb}";
        Map<String, String> map = new HashMap<>();
        map.put("aa", "cc");
        map.put("bb", "dd");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(6, list.get(0).length());
        Assert.assertEquals("aaccdd", list.get(0));
    }

    @Test
    public void testTransformExpression2() {
        String str = "${time()}aa${aa}${bb}";

        Map<String, String> map = new HashMap<>();
        map.put("aa", "cc");
        map.put("bb", "dd");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(19, list.get(0).length());
        Assert.assertEquals("aaccdd", list.get(0).substring(13));
    }

    @Test
    public void testTransformExpression3() {
        String str = "aa${aa}kk${time()}${bb}";

        Map<String, String> map = new HashMap<>();
        map.put("aa", "cc");
        map.put("bb", "dd");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(21, list.get(0).length());
        Assert.assertEquals("aacckkdd", list.get(0).substring(0, 6) + list.get(0).substring(19));
    }

    @Test
    public void testTransformExpression4() {
        String str = "aa${aa}kk${bb}${time()}";

        Map<String, String> map = new HashMap<>();
        map.put("aa", "cc");
        map.put("bb", "dd");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals(21, list.get(0).length());
        Assert.assertEquals("aacckkdd", list.get(0).substring(0, 8));
    }

    @Test
    public void testTransformExpression5() {
        String str = "${getJsonValues(${getVarFromJS(${js}, m)}, $.a)}";

        Map<String, String> map = new HashMap<>();
        map.put("js", "var m = {a:'c', b : 'd'};");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("c", list.get(0));
    }

    @Test
    public void testTransformExpression6() {
        String str = "${getJsonValues(${getVarFromJS(${js}, m)}, $.a)}aa${bb}";

        Map<String, String> map = new HashMap<>();
        map.put("js", "var m = {a:'c', b : 'd'};");
        map.put("bb", "cc");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("caacc", list.get(0));
    }

    @Test
    public void testTransformExpression7() {
        String str = "${bb}aa${getJsonValues(${getVarFromJS(${js}, m)}, $.a)}";

        Map<String, String> map = new HashMap<>();
        map.put("js", "var m = {a:'c', b : 'd'};");
        map.put("bb", "cc");

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("ccaac", list.get(0));
    }

    @Test
    public void testTransformExpression8() {
        String str = "${getJsonValues(${getVarFromJS(${js}, msgList)}, $.list[0].app_msg_ext_info.copyright_stat)}";

        Map<String, String> map = new HashMap<>();
        String js = "var msgList = {\"list\":[{\"app_msg_ext_info\":{\"copyright_stat\":100,\"del_flag\":1,\"digest\":\"唯一认证-汽车 一键关注车主玩转爱车、准车主欲买车官方唯一认证【汽车】必关注赞是一种鼓励 | 分享传递快乐关\"}}]};";
        map.put("js", js);

        List<String> list = new StatementEngine().transformExpression(str, map);
        Assert.assertEquals(1, list.size());
        Assert.assertEquals("100", list.get(0));
    }
}
