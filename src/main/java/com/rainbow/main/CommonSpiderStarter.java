package com.rainbow.main;

import com.rainbow.common.SpiderRequest;
import com.rainbow.config.SpiderConfig;
import com.rainbow.controller.SpiderController;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.File;
import java.util.Map;

/**
 * Created by xuming on 2017/9/29.
 */
public class CommonSpiderStarter {

    static {
        System.setProperty("SPIDER_LOG_HOME", System.getenv("SPIDER_HOME") + "/logs/common");
    }

    private static final Logger logger = LoggerFactory.getLogger(CommonSpiderStarter.class);

    public static void main(String[] args)  {
        try {
            String home = System.getenv("SPIDER_HOME");
            if (StringUtils.isBlank(home)) throw new RuntimeException("env SPIDER_HOME not exists!");

            String workDir = home + "/work/common";
            String setupFile = home + "/conf/common_spider_setup.json";

            initDir(workDir);

            SpiderController noProxyController = SpiderController.create(false, setupFile);
            SpiderController proxyController = SpiderController.create(true, setupFile);
            noProxyController.startSpider();
            proxyController.startSpider();

            Thread mainThread = Thread.currentThread();
            Signal.handle(new Signal("TERM"), new StopHandler(mainThread));

            Map<String, SpiderConfig> spiderConfigMap = noProxyController.getContext().getSpiderConfigMap();

            while (!mainThread.isInterrupted()) {
                try {
                    int count = 0;
                    for (SpiderConfig spiderConfig : spiderConfigMap.values()) {
                        if (count < 5) {
                            if (spiderConfig.enableProxy) {
                                proxyController = ExactSpiderStarter.restartIfStop(proxyController, true, setupFile);
                                proxyController.addSpiderRequest(new SpiderRequest(spiderConfig.id, null));
                            } else {
                                noProxyController = ExactSpiderStarter.restartIfStop(noProxyController, false, setupFile);
                                noProxyController.addSpiderRequest(new SpiderRequest(spiderConfig.id, null));
                            }

                            count++;
                        } else {
                            while (!proxyController.isFinished() && !noProxyController.isFinished()) {
                                Thread.sleep(1000 * 30);
                            }

                            count = 0;
                        }
                    }

                    logger.info("all common spider has completed, sleep 24h");
                    Thread.sleep(1000 * 60 * 60 * 24);
                } catch (InterruptedException e) {
                    logger.warn("accept exit signal, exit!");
                    mainThread.interrupt();
                }
            }

            proxyController.waitUntilFinish();
            noProxyController.waitUntilFinish();

            proxyController.destroy();
            noProxyController.destroy();
        } catch (Exception e) {
            logger.error("exception happened!", e);
        }

    }

    private static void initDir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
