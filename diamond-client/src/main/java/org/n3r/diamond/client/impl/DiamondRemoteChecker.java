package org.n3r.diamond.client.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.client.DiamondListener;
import org.n3r.diamond.client.DiamondStone;
import org.n3r.diamond.client.cache.DiamondCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.TimeUnit;

class DiamondRemoteChecker {
    private final DiamondCache diamondCache;
    private Logger log = LoggerFactory.getLogger(DiamondRemoteChecker.class);

    private Cache<DiamondStone.DiamondAxis, String> contentCache = CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(1000)
            .build();

    private volatile DiamondAllListener diamondAllListener = new DiamondAllListener();


    private DiamondHttpClient httpClient;
    private final DiamondManagerConf managerConfig;
    private final DiamondSubscriber diamondSubscriber;

    public DiamondRemoteChecker(DiamondSubscriber diamondSubscriber,
                                DiamondManagerConf managerConfig,
                                DiamondCache diamondCache) {
        this.diamondSubscriber = diamondSubscriber;
        this.managerConfig = managerConfig;
        this.diamondCache = diamondCache;

        httpClient = new DiamondHttpClient(managerConfig);
        managerConfig.randomDomainNamePos();
    }

    public void addDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener diamondListener) {
        diamondAllListener.addDiamondListener(diamondAxis, diamondListener);
    }

    public void removeDiamondListener(DiamondStone.DiamondAxis diamondAxis, DiamondListener diamondListener) {
        diamondAllListener.removeDiamondListener(diamondAxis, diamondListener);
    }


    public void shutdown() {
        httpClient.shutdown();
    }

    public void checkRemote() {
        Set<String> updateDataIdGroupPairs = checkUpdateDataIds(managerConfig.getReceiveWaitTime());
        if (null == updateDataIdGroupPairs || updateDataIdGroupPairs.size() == 0) {
            log.debug("没有被修改的DataID");
            return;
        }

        // 对于每个发生变化的DataID，都请求一次对应的配置信息
        for (String freshDataIdGroupPair : updateDataIdGroupPairs) {
            int middleIndex = freshDataIdGroupPair.indexOf(Constants.WORD_SEPARATOR);
            if (middleIndex == -1) continue;

            String freshDataId = freshDataIdGroupPair.substring(0, middleIndex);
            String freshGroup = freshDataIdGroupPair.substring(middleIndex + 1);

            DiamondMeta diamondMeta = diamondSubscriber.getCacheData(DiamondStone.DiamondAxis.makeAxis(freshGroup, freshDataId));

            receiveDiamondContent(diamondMeta);
        }
    }

    /**
     * 向DiamondServer请求dataId对应的配置信息，并将结果抛给客户的监听器
     */
    private void receiveDiamondContent(final DiamondMeta diamondMeta) {
        try {
            retriveRemoteAndInvokeListeners(diamondMeta);
        } catch (Exception e) {
            log.error("向Diamond服务器索要配置信息的过程抛异常", e.getMessage());
        }
    }

    private void retriveRemoteAndInvokeListeners(DiamondMeta diamondMeta) {
        String diamondContent = retrieveRemote(diamondMeta.getDiamondAxis(),
                managerConfig.getReceiveWaitTime(), false);
        if (null == diamondContent) return;

        if (null == diamondAllListener) {
            log.warn("null == configInfoListenable");
            return;
        }

        onDiamondChanged(diamondMeta, diamondContent);
    }


    void onDiamondChanged(final DiamondMeta diamondMeta, final String content) {
        final DiamondStone diamondStone = new DiamondStone();
        diamondStone.setContent(content);
        diamondStone.setDiamondAxis(diamondMeta.getDiamondAxis());
        diamondMeta.incSuccCounterAndGet();

        Runnable command = new Runnable() {
            public void run() {
                try {
                    diamondSubscriber.saveSnapshot(diamondStone.getDiamondAxis(), content);
                    diamondCache.saveDiamondCache(diamondStone.getDiamondAxis(), content);
                    diamondAllListener.accept(diamondStone);
                } catch (Throwable t) {
                    log.error("配置信息监听器中有异常，{}", diamondMeta.getDiamondAxis(), t);
                }
            }
        };

        if (null != diamondAllListener.getExecutor()) {
            diamondAllListener.getExecutor().execute(command);
        } else {
            command.run();
        }
    }

    /**
     * @param useContentCache 是否使用本地的内容cache。
     *                        主动get时会使用，有check触发的异步get不使用本地cache。
     */
    String retrieveRemote(DiamondStone.DiamondAxis diamondAxis, long timeout, boolean useContentCache) {
        if (MockDiamondServer.isTestMode()) return MockDiamondServer.getDiamond(diamondAxis);

        diamondSubscriber.start();

        if (useContentCache) {
            String content = contentCache.getIfPresent(diamondAxis);
            if (content != null) return content;
        }

        long waitTime = 0;

        String uri = getUriString(diamondAxis);
        log.info(uri);


        int totalRetryTimes = managerConfig.getRetrieveDataRetryTimes();
        int triedTimes = 0;

        while (0 == timeout || timeout > waitTime) {
            if (++triedTimes > totalRetryTimes + 1) {
                log.warn("已经到达了设定的重试次数");
                break;
            }

            log.info("获取配置数据，第{}次尝试, waitTime：{}", triedTimes, waitTime);

            // 设置超时时间
            long onceTimeOut = getOnceTimeOut(waitTime, timeout);
            waitTime += onceTimeOut;

            try {
                DiamondMeta diamondMeta = diamondSubscriber.getCacheData(diamondAxis);
                DiamondHttpClient.GetDiamondResult getDiamondResult;
                getDiamondResult = httpClient.getDiamond(uri, useContentCache, diamondMeta, onceTimeOut);

                switch (getDiamondResult.getHttpStatus()) {
                    case Constants.SC_OK:
                        return getSuccess(diamondAxis, diamondMeta, getDiamondResult);
                    case Constants.SC_NOT_MODIFIED:
                        return getNotModified(diamondAxis, diamondMeta, getDiamondResult);
                    case Constants.SC_NOT_FOUND:
                        log.warn("没有找到{}对应的配置信息", diamondAxis);
                        diamondMeta.setMd5(Constants.NULL);
                        diamondSubscriber.removeSnapshot(diamondAxis);
                        diamondCache.removeCacheSnapshot(diamondAxis);
                        contentCache.invalidate(diamondAxis);
                        return null;
                    case Constants.SC_SERVICE_UNAVAILABLE:
                        managerConfig.rotateToNextDomain();
                        break;

                    default: {
                        log.warn("HTTP State: {} : {} ", getDiamondResult.getHttpStatus(),
                                httpClient.getState());
                        managerConfig.rotateToNextDomain();
                    }
                }
            } catch (Exception e) {
                log.error("获取配置信息IO异常：{}", e.getMessage());
                managerConfig.rotateToNextDomain();
            }
        }

        throw new RuntimeException("获取ConfigureInfomation超时," + diamondAxis + ", 超时时间=" + timeout);
    }

    /**
     * 从DiamondServer获取值变化了的DataID列表
     */
    private Set<String> checkUpdateDataIds(long timeout) {
        if (MockDiamondServer.isTestMode()) return null;

        long waitTime = 0;

        String probeUpdateString = diamondSubscriber.createProbeUpdateString();
        if (StringUtils.isBlank(probeUpdateString)) return null;

        while (0 == timeout || timeout > waitTime) {
            // 设置超时时间
            long onceTimeOut = getOnceTimeOut(waitTime, timeout);
            waitTime += onceTimeOut;

            try {
                DiamondHttpClient.CheckResult checkResult;
                checkResult = httpClient.checkUpdateDataIds(probeUpdateString, onceTimeOut);
                int httpStatus = checkResult.getHttpStatus();
                switch (httpStatus) {
                    case Constants.SC_OK:
                        return checkResult.getUpdateDataIdsInBody();
                    case Constants.SC_SERVICE_UNAVAILABLE:
                        managerConfig.rotateToNextDomain();
                        break;
                    default:
                        log.warn("获取修改过的DataID列表的请求回应的HTTP State: " + httpStatus);
                        managerConfig.rotateToNextDomain();
                }
            } catch (Exception e) {
                log.error("未知异常:{}", e.getMessage());
                managerConfig.rotateToNextDomain();
            }
        }
        throw new RuntimeException("获取修改过的DataID列表超时 "
                + managerConfig.getDomainName()
                + ", 超时时间为：" + timeout);
    }


    /**
     * 回馈的结果为RP_OK，则整个流程为：<br>
     * 1.获取配置信息，如果配置信息为空或者抛出异常，则抛出运行时异常<br>
     * 2.检测配置信息是否符合回馈结果中的MD5码，不符合，则再次获取配置信息，并记录日志<br>
     * 3.符合，则存储LastModified信息和MD5码，调整查询的间隔时间，将获取的配置信息发送给客户的监听器<br>
     */
    private String getSuccess(DiamondStone.DiamondAxis diamondAxis, DiamondMeta diamondMeta,
                              DiamondHttpClient.GetDiamondResult httpMethod) {
        String diamondContent = httpMethod.getResponseContent();

        if (!DiamondUtils.checkMd5(diamondContent, httpMethod.getMd5())) {
            throw new RuntimeException("配置信息的MD5码校验出错,DataID为:["
                    + diamondAxis.getDataId() + "]配置信息为:[" + diamondContent + "]MD5为:[" + httpMethod.getMd5() + "]");
        }

        String lastModified = httpMethod.getLastModified();

        diamondMeta.setMd5(httpMethod.getMd5());
        diamondMeta.setLastModifiedHeader(lastModified);

        changeSpacingInterval(httpMethod);

        contentCache.put(diamondAxis, diamondContent);

        log.info("接收到的数据{}, content={}", diamondAxis, diamondContent);

        return diamondContent;
    }

    /**
     * @param waitTime 本次查询已经耗费的时间(已经查询的多次HTTP耗费的时间)
     * @param timeout  本次查询总的可耗费时间(可供多次HTTP查询使用)
     * @return 本次HTTP查询能够使用的时间
     */
    long getOnceTimeOut(long waitTime, long timeout) {
        long onceTimeOut = this.managerConfig.getOnceTimeout();
        long remainTime = timeout - waitTime;
        if (onceTimeOut > remainTime) onceTimeOut = remainTime;

        return onceTimeOut;
    }

    /**
     * 回馈的结果为RP_NO_CHANGE，则整个流程为：<br>
     * 1.检查缓存中的MD5码与返回的MD5码是否一致，如果不一致，则删除缓存行。重新再次查询。<br>
     * 2.如果MD5码一致，则直接返回NULL<br>
     */
    private String getNotModified(DiamondStone.DiamondAxis diamondAxis, DiamondMeta diamondMeta,
                                  DiamondHttpClient.GetDiamondResult httpMethod) {
        String md5 = httpMethod.getMd5();
        if (!diamondMeta.getMd5().equals(md5)) {
            String lastMd5 = diamondMeta.getMd5();
            diamondMeta.setMd5(Constants.NULL);
            diamondMeta.setLastModifiedHeader(Constants.NULL);
            throw new RuntimeException("MD5码校验对比出错,[" + diamondAxis
                    + "]上次MD5为:[" + lastMd5 + "]本次MD5为:[" + md5 + "]");
        }

        diamondMeta.setMd5(md5);
        changeSpacingInterval(httpMethod);
        log.info("{}, 对应的DiamondContent没有变化", diamondAxis);
        return null;
    }

    /**
     * 设置新的消息轮询间隔时间
     */
    void changeSpacingInterval(DiamondHttpClient.GetDiamondResult httpMethod) {
        int pollingIntervalTime = httpMethod.getPollingInterval();
        if (pollingIntervalTime > 0) managerConfig.setPollingInterval(pollingIntervalTime);
    }

    /**
     * 获取查询Uri的String
     */
    String getUriString(DiamondStone.DiamondAxis diamondAxis) {
        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(Constants.HTTP_URI_FILE)
                .append("?" + Constants.DATAID + "=" + diamondAxis.getDataId())
                .append("&" + Constants.GROUP + "=" + diamondAxis.getGroup());

        return uriBuilder.toString();
    }
}
