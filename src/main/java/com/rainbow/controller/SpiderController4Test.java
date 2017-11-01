package com.rainbow.controller;

import com.rainbow.common.SpiderRequest;
import com.rainbow.config.SetupConfig;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xuming on 2017/10/14.
 */
public class SpiderController4Test {

    static {
        System.setProperty("TEST_MODE", "true");
    }

    private static final Logger logger = LoggerFactory.getLogger(SpiderController4Test.class);

    private SpiderController controller = null;

    private SpiderController4Test(SetupConfig setupFile) throws Exception {
        if (StringUtils.isNotBlank(setupFile.proxyHost)) {
            controller = SpiderController.create(true, setupFile);
        } else {
            controller = SpiderController.create(false, setupFile);
        }
    }

    public static SpiderController4Test create(String spiderDir) throws Exception {
        SetupConfig setupFile = SetupConfig.create4Test();
        setupFile.spiderDir = spiderDir;

        return new SpiderController4Test(setupFile);
    }

    public static SpiderController4Test create(String spiderDir, String proxyHost) throws Exception {
        SetupConfig setupFile = SetupConfig.create4Test();
        setupFile.spiderDir = spiderDir;
        setupFile.proxyHost = proxyHost;

        return new SpiderController4Test(setupFile);
    }

    public void startSpider() {
        controller.startSpider();
    }

    public void addSpiderRequest(SpiderRequest request) throws InterruptedException {
        controller.addSpiderRequest(request);
    }

    public void waitUntilFinish() {
        controller.waitUntilFinish();
    }

    public static void main(String[] args) {
        try {
            String proxy = "http://dynamic.goubanjia.com/dynamic/get/0f3d54c09a84a98a2223f35551c28e20.html";
            SpiderController4Test controller = SpiderController4Test.create("spiders/extract");
            controller.startSpider();

            Map<String,String> params = new HashMap<>();
            params.put("businesscode", "1kma5xo");
            SpiderRequest spiderRequest = new SpiderRequest("01N004", params);
            controller.addSpiderRequest(spiderRequest);

            controller.waitUntilFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
