package org.n3r.diamond.client.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.client.DiamondListener;
import org.n3r.diamond.client.DiamondStone;
import org.n3r.diamond.client.cache.DiamondCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DiamondSubscriber implements Closeable {
    private static DiamondSubscriber instance = new DiamondSubscriber();

    public static DiamondSubscriber getInstance() {
        return instance;
    }

    private Logger log = LoggerFactory.getLogger(DiamondSubscriber.class);

    private final LoadingCache<DiamondStone.DiamondAxis, DiamondMeta> metaCache
            = CacheBuilder.newBuilder()
            .build(new CacheLoader<DiamondStone.DiamondAxis, DiamondMeta>() {
                @Override
                public DiamondMeta load(DiamondStone.DiamondAxis key) throws Exception {
                    start();
                    return new DiamondMeta(key);
                }
            });

    private volatile DiamondManagerConf managerConfig = new DiamondManagerConf();

    private ScheduledExecutorService scheduler;
    private LocalDiamondMiner localDiamondMiner = new LocalDiamondMiner();
    private ServerAddrMiner serverAddrMiner;
    private SnapshotMiner snapshotMiner;
    private DiamondCache diamondCache;

    private volatile boolean running;

    private DiamondRemoteChecker diamondRemoteChecker;

    private DiamondSubscriber() {
    }

    public void addDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener diamondListener) {
        diamondRemoteChecker.addDiamondListener(diamondAxis, diamondListener);
    }


    public void removeDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener diamondListener) {
        diamondRemoteChecker.removeDiamondListener(diamondAxis, diamondListener);
    }

    public synchronized void start() {
        if (running) return;

        if (null == scheduler || scheduler.isTerminated())
            scheduler = Executors.newSingleThreadScheduledExecutor();

        localDiamondMiner.start(managerConfig);

        serverAddrMiner = new ServerAddrMiner(managerConfig, scheduler);
        serverAddrMiner.start();

        snapshotMiner = new SnapshotMiner(managerConfig);
        diamondCache = new DiamondCache(snapshotMiner);

        diamondRemoteChecker = new DiamondRemoteChecker(this, managerConfig, diamondCache);

        running = true;

        log.info("name servers {}", managerConfig.getNameServers());

        rotateCheckDiamonds();

        addShutdownHook();
    }


    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                close();
            }
        });
    }

    private void rotateCheckDiamonds() {
        int pollingInterval = managerConfig.getPollingInterval();
        scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                rotateCheckDiamonsTask();
            }

        }, pollingInterval, pollingInterval, TimeUnit.SECONDS);
    }

    private void rotateCheckDiamonsTask() {
        try {
            checkLocal();
            diamondRemoteChecker.checkRemote();
            checkSnapshot();
        } catch (Exception e) {
            log.error("rotateCheckDiamonsTask error {}", e.getMessage());
        }
    }


    @Override
    public synchronized void close() {
        if (!running) return;
        running = false;

        log.warn("start to close DiamondSubscriber");

        localDiamondMiner.stop();
        serverAddrMiner.stop();

        scheduler.shutdownNow();

        metaCache.invalidateAll();
        diamondRemoteChecker.shutdown();
        diamondCache.close();

        log.warn("end to close DiamondSubscriber");
    }


    public String retrieveDiamondLocalAndRemote(DiamondStone.DiamondAxis diamondAxis, long timeout) {
        DiamondMeta diamondMeta = getCacheData(diamondAxis);
        // local first
        try {
            String localConfig = localDiamondMiner.readLocal(diamondMeta);
            if (localConfig != null) {
                diamondMeta.incSuccCounterAndGet();
                saveSnapshot(diamondAxis, localConfig);
                return localConfig;
            }
        } catch (Exception e) {
            log.error("get local error", e);
        }

        String result = diamondRemoteChecker.retrieveRemote(diamondAxis, timeout, true);
        if (result != null) {
            saveSnapshot(diamondAxis, result);
            diamondMeta.incSuccCounterAndGet();
        }

        return result;
    }

    public void saveSnapshot(DiamondStone.DiamondAxis diamondAxis, String diamondContent) {
        snapshotMiner.saveSnaptshot(diamondAxis, diamondContent);
    }

    public String getDiamond(DiamondStone.DiamondAxis diamondAxis, long timeout) {
        try {
            String result = retrieveDiamondLocalAndRemote(diamondAxis, timeout);
            if (StringUtils.isNotBlank(result)) return result;
        } catch (Exception t) {
            log.error(t.getMessage());
        }

        if (MockDiamondServer.isTestMode()) return null;

        return getSnapshot(diamondAxis);
    }


    public String getSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        try {
            DiamondMeta diamondMeta = getCacheData(diamondAxis);
            String diamondContent = snapshotMiner.getSnapshot(diamondAxis);
            if (diamondContent != null && diamondMeta != null) diamondMeta.incSuccCounterAndGet();

            return diamondContent;
        } catch (Exception e) {
            log.error("getSnapshot error diamondAxis={}", diamondAxis, e);
            return null;
        }
    }

    public void removeSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        snapshotMiner.removeSnapshot(diamondAxis);
    }

    public void checkSnapshot() {
        for (Map.Entry<DiamondStone.DiamondAxis, DiamondMeta> entry : metaCache.asMap().entrySet()) {
            final DiamondMeta diamondMeta = entry.getValue();

            if (diamondMeta.isUseLocal()) continue;
            if (diamondMeta.getFetchCount() > 0) continue;

            String diamond = getSnapshot(diamondMeta.getDiamondAxis());
            if (diamond != null) diamondRemoteChecker.onDiamondChanged(diamondMeta, diamond);
        }
    }

    public DiamondMeta getCacheData(DiamondStone.DiamondAxis diamondAxis) {
        return metaCache.getUnchecked(diamondAxis);
    }

    public void checkLocal() {
        for (Map.Entry<DiamondStone.DiamondAxis, DiamondMeta> entry : metaCache.asMap().entrySet()) {
            final DiamondMeta diamondMeta = entry.getValue();

            try {
                String content = localDiamondMiner.checkLocal(diamondMeta);
                if (null != content) {
                    log.info("local config read, {}", diamondMeta.getDiamondAxis());

                    diamondRemoteChecker.onDiamondChanged(diamondMeta, content);
                }
            } catch (Exception e) {
                log.error("check local error", e);
            }
        }
    }


    public String createProbeUpdateString() {
        StringBuilder probeModifyBuilder = new StringBuilder();
        for (Map.Entry<DiamondStone.DiamondAxis, DiamondMeta> entry : metaCache.asMap().entrySet()) {
            final DiamondMeta data = entry.getValue();
            if (data.isUseLocal()) continue;

            DiamondStone.DiamondAxis axis = data.getDiamondAxis();

            probeModifyBuilder.append(axis.getDataId())
                    .append(Constants.WORD_SEPARATOR).append(axis.getGroup())
                    .append(Constants.WORD_SEPARATOR).append(data.getMd5())
                    .append(Constants.LINE_SEPARATOR);
        }

        return probeModifyBuilder.toString();
    }

    public Object getCache(DiamondStone.DiamondAxis diamondAxis, int timeoutMillis) {
        String diamondContent = getDiamond(diamondAxis, timeoutMillis);
        if (diamondContent == null) return null;

        return diamondCache.getCache(diamondAxis, diamondContent);
    }
}
