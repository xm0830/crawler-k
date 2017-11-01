package com.rainbow.controller;

import com.rainbow.common.SpiderRequest;
import com.rainbow.config.SetupConfig;
import com.rainbow.config.SpiderConfig;
import com.rainbow.spiders.SharkSpider;
import com.rainbow.tool.StatementEngine;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.Frontier;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xuming on 2017/9/25.
 */
public class SpiderController extends CrawlController {

    private static final Logger logger = LoggerFactory.getLogger(SpiderController.class);

    private int threadNum = 1;
    private ControllerContext context = null;

    private SpiderController(CrawlConfig config, PageFetcher pageFetcher, RobotstxtServer robotstxtServer, SetupConfig setupConfig) throws Exception {
        super(config, pageFetcher, robotstxtServer);
        this.threadNum = setupConfig.workers;

        Map<String, SpiderConfig> spiderConfigMap = loadSpiderConfigs(setupConfig.spiderDir);
        this.context = new ControllerContext(spiderConfigMap, setupConfig);
    }


    public static SpiderController create(boolean hasProxy, String setupFile) throws Exception {
        SetupConfig setupConfig = SetupConfig.load(new File(setupFile));
        return create(hasProxy, setupConfig);
    }

    public static SpiderController create(boolean hasProxy, SetupConfig setupConfig) throws Exception {

        if (!setupConfig.check()) throw new RuntimeException("setup config format error!");

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(setupConfig.dataDir);
        config.setUserAgentString("Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.79 Safari/537.36");
        config.setDefaultHeaders(setupConfig.request.getHeaders());
        config.setPolitenessDelay(setupConfig.request.delayTime);
        config.setIncludeBinaryContentInCrawling(setupConfig.includeBinary);
        config.setResumableCrawling(setupConfig.resume);
        config.setShutdownOnEmptyQueue(setupConfig.stopOnEmpty);
        config.setCleanupDelaySeconds(10);

        if (hasProxy) {
            config.setCrawlStorageFolder(setupConfig.dataDir + "_proxy");
            config.setProxyHost(setupConfig.proxyHost);
            config.setProxyPort(setupConfig.proxyPort);

            setupConfig.workers = 1;
        }

        PageFetcher fetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, fetcher);

        return new SpiderController(config, fetcher, robotstxtServer, setupConfig);
    }

    public void addSpiderRequest(SpiderRequest request) throws InterruptedException {
        if (request.check()) {
            Map<String, SpiderConfig> spiderConfigMap = context.getSpiderConfigMap();
            if (spiderConfigMap.containsKey(request.spiderId)) {
                SpiderConfig spiderConfig = spiderConfigMap.get(request.spiderId);
                List<String> list = new StatementEngine().transformExpression(spiderConfig.seedUrl, request.params);
                for (String seed : list) {
                    Frontier frontier = getFrontier();
                    while (!frontier.isFinished() && frontier.getQueueLength() > 100) {
                        Thread.sleep(1000 * 2);
                    }

                    logger.debug("add seed: " + seed);
                    addSeed(seed, request.spiderId);
                }
            } else {
                logger.warn("there is no spider for id: {}", request.spiderId);
            }
        } else {
            logger.error("error spider config format!");
        }
    }

    public ControllerContext getContext() {
        return context;
    }

    public void destroy() {
        context.reset();
    }

    public void startSpider() {
        context.reset();
        startNonBlocking(SharkSpider.class, threadNum);
    }

    private static Map<String, SpiderConfig> loadSpiderConfigs(String spiderConfigDir) {
        final Map<String, SpiderConfig> spiderConfigMap = new HashMap<>();
        File file = new File(spiderConfigDir);
        if (file.exists()) {
            file.listFiles(new FileFilter() {

                @Override
                public boolean accept(File pathname) {

                    try {
                        String file = pathname.getCanonicalPath();
                        if (file.endsWith(".json")) {
                            SpiderConfig spiderConfig = SpiderConfig.load(pathname);

                            if (spiderConfig.check()) {
                                spiderConfigMap.put(spiderConfig.id, spiderConfig);
                            } else {
                                throw new RuntimeException("spider config error! config: " + file);
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    return false;
                }
            });
        } else {
            throw new RuntimeException("spider config dir:" + spiderConfigDir + " not exists!");
        }

        return spiderConfigMap;
    }
}
