package com.rainbow.main;

import com.rainbow.common.DataMessage;
import com.rainbow.common.DataQueue;
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
 * Created by xuming on 2017/9/27.
 */
public class ExactSpiderStarter {

    static {
        System.setProperty("SPIDER_LOG_HOME", System.getenv("SPIDER_HOME") + "/logs/extract");
    }

    private static final Logger logger = LoggerFactory.getLogger(ExactSpiderStarter.class);

    public static void main(String[] args) {
        try {
            String home = System.getenv("SPIDER_HOME");
            if (StringUtils.isBlank(home)) throw new RuntimeException("env SPIDER_HOME not exists!");

            String workDir = home + "/work/extract";
            String setupFile = home + "/conf/extract_spider_setup.json";

            initDir(workDir);

            SpiderController noProxyController = SpiderController.create(false, setupFile);
            SpiderController proxyController = SpiderController.create(true, setupFile);

            noProxyController.startSpider();
            proxyController.startSpider();

            final DataQueue queue = new DataQueue(100000);
            queue.selectAndAuth();

            Thread mainThread = Thread.currentThread();

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    queue.close();
                }
            });
            Signal.handle(new Signal("TERM"), new StopHandler(mainThread));

            Map<String, SpiderConfig> spiderConfigMap = proxyController.getContext().getSpiderConfigMap();
            while (!mainThread.isInterrupted()) {
                try {
                    String info = queue.pull();
                    logger.debug("get spider info from queue: " + info);

                    DataMessage dataMessage = DataMessage.create(info);
                    if (spiderConfigMap.containsKey(dataMessage.spiderId)) {
                        if (spiderConfigMap.get(dataMessage.spiderId).enableProxy) {
                            logger.info("schedule {} to proxy spider", dataMessage.spiderId);
                            proxyController = restartIfStop(proxyController, true, setupFile);
                            proxyController.addSpiderRequest(new SpiderRequest(dataMessage.spiderId, dataMessage.params));
                        } else {
                            logger.info("schedule {} to non proxy spider", dataMessage.spiderId);
                            noProxyController = restartIfStop(noProxyController, false, setupFile);
                            noProxyController.addSpiderRequest(new SpiderRequest(dataMessage.spiderId, dataMessage.params));
                        }
                    }
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

    public static SpiderController restartIfStop(SpiderController controller, boolean hasProxy, String setupFile) throws Exception {
        while (!controller.isFinished() && controller.isShuttingDown()) {
            Thread.sleep(1000 * 2);
        }

        if (controller.isFinished()) {
            controller.destroy();
            Thread.sleep(1000 * 2);

            controller = SpiderController.create(hasProxy, setupFile);
            controller.startSpider();
        }

        return controller;
    }

    private static void initDir(String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}
