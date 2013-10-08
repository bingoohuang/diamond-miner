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
import java.io.IOException;
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

    /**
     * 启动DiamondSubscriber：<br>
     * 1.阻塞主动获取所有的DataId配置信息<br>
     * 2.启动定时线程定时获取所有的DataId配置信息<br>
     */
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

        running = true; // 初始化完毕

        log.info("当前使用的域名有：{}", managerConfig.getDomainNames());

        rotateCheckDiamonds();

        addShutdownHook();
    }


    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                close();   // 关闭单例订阅者
            }
        });
    }

    /**
     * 循环探测配置信息是否变化，如果变化，则再次向DiamondServer请求获取对应的配置信息
     */
    private void rotateCheckDiamonds() {
        scheduler.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                rotateCheckDiamonsTask();
            }

        }, managerConfig.getPollingInterval(),
                managerConfig.getPollingInterval(), TimeUnit.SECONDS);
    }

    private void rotateCheckDiamonsTask() {
        if (!running) {
            log.warn("DiamondSubscriber不在运行状态中，退出查询循环");
            return;
        }

        try {
            checkLocal();
            diamondRemoteChecker.checkRemote();
            checkSnapshot();
        } catch (Exception e) {
            log.error("循环探测发生异常:{}", e.getMessage());
        }
    }


    @Override
    public synchronized void close() {
        if (!running) return;
        running = false;

        log.warn("开始关闭DiamondSubscriber");

        localDiamondMiner.stop();
        serverAddrMiner.stop();

        scheduler.shutdownNow();

        metaCache.invalidateAll();
        diamondRemoteChecker.shutdown();
        diamondCache.close();

        log.warn("完成关闭DiamondSubscriber");
    }


    public String getDiamondLocalFirst(DiamondStone.DiamondAxis diamondAxis, long timeout) {
        DiamondMeta diamondMeta = getCacheData(diamondAxis);
        // 优先使用本地配置
        try {
            String localConfig = localDiamondMiner.readLocal(diamondMeta);
            if (localConfig != null) {
                diamondMeta.incSuccCounterAndGet();
                saveSnapshot(diamondAxis, localConfig);
                return localConfig;
            }
        } catch (Exception e) {
            log.error("获取本地配置文件出错", e);
        }

        // 获取本地配置失败，从网络取
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

    /**
     * 同步获取一份有效的配置信息，按照<strong>本地文件->diamond服务器->上一次正确配置的snapshot</strong>
     * 的优先顺序获取， 如果这些途径都无效，则返回null
     */
    public String getDiamond(DiamondStone.DiamondAxis diamondAxis, long timeout) {
        // 尝试先从本地和网络获取配置信息
        try {
            String result = getDiamondLocalFirst(diamondAxis, timeout);
            if (StringUtils.isNotBlank(result)) return result;
        } catch (Exception t) {
            log.error(t.getMessage());
        }

        // 测试模式不使用本地dump
        if (MockDiamondServer.isTestMode()) return null;

        return getSnapshot(diamondAxis);
    }

    /**
     * 同步获取一份有效的配置信息，按照<strong>上一次正确配置的snapshot->本地文件->diamond服务器</strong>
     * 的优先顺序获取， 如果这些途径都无效，则返回null
     */
    public String getDiamondSnapshotFirst(DiamondStone.DiamondAxis diamondAxis, long timeout) {
        String result = getSnapshot(diamondAxis);
        if (StringUtils.isNotBlank(result)) return result;

        return getDiamondLocalFirst(diamondAxis, timeout);
    }

    public String getSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        try {
            DiamondMeta diamondMeta = getCacheData(diamondAxis);
            String diamondContent = snapshotMiner.getSnapshot(diamondAxis);
            if (diamondContent != null && diamondMeta != null) diamondMeta.incSuccCounterAndGet();

            return diamondContent;
        } catch (Exception e) {
            log.error("获取snapshot出错， diamondAxis={}", diamondAxis, e);
            return null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void removeSnapshot(DiamondStone.DiamondAxis diamondAxis) {
        snapshotMiner.removeSnapshot(diamondAxis);
    }

    public void checkSnapshot() {
        for (Map.Entry<DiamondStone.DiamondAxis, DiamondMeta> entry : metaCache.asMap().entrySet()) {
            final DiamondMeta diamondMeta = entry.getValue();

            // 没有获取本地配置，也没有从diamond server获取配置成功,则加载上一次的snapshot
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
                    log.info("本地配置信息被读取, {}", diamondMeta.getDiamondAxis());

                    diamondRemoteChecker.onDiamondChanged(diamondMeta, content);
                }
            } catch (Exception e) {
                log.error("向本地索要配置信息的过程抛异常", e);
            }
        }
    }


    /**
     * 获取探测更新的DataID的请求字符串(获取check的DataID:Group:MD5串)
     */
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
