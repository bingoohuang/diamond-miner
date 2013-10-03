package org.n3r.diamond.client.impl;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.n3r.diamond.client.impl.Constants.*;

public class DiamondManagerConf {
    private volatile int pollingInterval = POLLING_INTERVAL; // 异步查询的间隔时间
    private volatile int onceTimeout = ONCE_TIMEOUT; // 获取对于一个DiamondServer所对应的查询一个DataID对应的配置信息的Timeout时间
    private volatile int receiveWaitTime = RECV_WAIT_TIMEOUT; // 同步查询一个DataID所花费的时间

    private volatile List<String> domainNames = new LinkedList<String>();

    private boolean localFirst = false;

    // 以下参数不支持运行后动态更新
    private int maxHostConnections = 1;
    private boolean connectionStaleCheckingEnabled = true;
    private int maxTotalConnections = 20;
    private int connectionTimeout = CONN_TIMEOUT;
    // 获取数据时的重试次数
    private int retrieveDataRetryTimes = Integer.MAX_VALUE / 10;


    // 本地数据保存路径
    private String filePath;

    public DiamondManagerConf() {
        filePath = System.getProperty("user.home") + File.separator + ".diamond-client";
        File dir = new File(filePath);
        dir.mkdirs();
        if (!dir.exists()) throw new RuntimeException("创建diamond-miner目录失败：" + filePath);
    }


    /**
     * 获取和同一个DiamondServer的最大连接数
     */
    public int getMaxHostConnections() {
        return maxHostConnections;
    }


    /**
     * 设置和同一个DiamondServer的最大连接数<br>
     * 不支持运行时动态更新
     */
    public void setMaxHostConnections(int maxHostConnections) {
        this.maxHostConnections = maxHostConnections;
    }


    /**
     * 是否允许对陈旧的连接情况进行检测。<br>
     * 如果不检测，性能上会有所提升，但是，会有使用不可用连接的风险导致的IO Exception
     */
    public boolean isConnectionStaleCheckingEnabled() {
        return connectionStaleCheckingEnabled;
    }


    /**
     * 设置是否允许对陈旧的连接情况进行检测。<br>
     * 不支持运行时动态更新
     */
    public void setConnectionStaleCheckingEnabled(boolean connectionStaleCheckingEnabled) {
        this.connectionStaleCheckingEnabled = connectionStaleCheckingEnabled;
    }


    /**
     * 获取允许的最大的连接数量。
     */
    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }


    /**
     * 设置允许的最大的连接数量。<br>
     * 不支持运行时动态更新
     */
    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
    }


    /**
     * 获取轮询的间隔时间。单位：秒<br>
     * 此间隔时间代表轮询查找一次配置信息的间隔时间，对于容灾相关，请设置短一些；<br>
     * 对于其他不可变的配置信息，请设置长一些
     */
    public int getPollingInterval() {
        return pollingInterval;
    }


    /**
     * 设置轮询的间隔时间。单位：秒
     */
    public void setPollingInterval(int pollingInterval) {
        if (pollingInterval < POLLING_INTERVAL && !MockDiamondServer.isTestMode()) return;
        this.pollingInterval = pollingInterval;
    }


    /**
     * 获取当前支持的所有的DiamondServer域名列表
     */
    public List<String> getDomainNames() {
        return domainNames;
    }

    public boolean hasDiamondServers() {
        return domainNames.size() > 0;
    }

    /**
     * 设置当前支持的所有的DiamondServer域名列表，当设置了域名列表后，缺省的域名列表将失效
     */
    public void setDomainNames(List<String> domainNames) {
        this.domainNames = new LinkedList<String>(domainNames);
    }


    /**
     * 添加一个DiamondServer域名，当设置了域名列表后，缺省的域名列表将失效
     */
    public void addDomainName(String domainName) {
        this.domainNames.add(domainName);
    }


    /**
     * 获取探测本地文件的路径
     */
    public String getFilePath() {
        return filePath;
    }


    /**
     * 设置探测本地文件的路径<br>
     * 不支持运行时动态更新
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    /**
     * 获取对于一个DiamondServer所对应的查询一个DataID对应的配置信息的Timeout时间<br>
     * 即一次HTTP请求的超时时间<br>
     * 单位：毫秒<br>
     */
    public int getOnceTimeout() {
        return onceTimeout;
    }


    /**
     * 设置对于一个DiamondServer所对应的查询一个DataID对应的配置信息的Timeout时间<br>
     * 单位：毫秒<br>
     * 配置信息越大，请将此值设置得越大
     */
    public void setOnceTimeout(int onceTimeout) {
        this.onceTimeout = onceTimeout;
    }


    /**
     * 获取和DiamondServer的连接建立超时时间。单位：毫秒
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }


    /**
     * 设置和DiamondServer的连接建立超时时间。单位：毫秒<br>
     * 不支持运行时动态更新
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }


    /**
     * 同步查询一个DataID的最长等待时间<br>
     * 实际最长等待时间小于receiveWaitTime + min(connectionTimeout, onceTimeout)
     */
    public int getReceiveWaitTime() {
        return receiveWaitTime;
    }


    /**
     * 设置一个DataID的最长等待时间<br>
     * 实际最长等待时间小于receiveWaitTime + min(connectionTimeout, onceTimeout)
     * 建议此值设置为OnceTimeout * （DomainName个数 + 1）
     */
    public void setReceiveWaitTime(int receiveWaitTime) {
        this.receiveWaitTime = receiveWaitTime;
    }


    public int getRetrieveDataRetryTimes() {
        return retrieveDataRetryTimes;
    }


    public void setRetrieveDataRetryTimes(int retrieveDataRetryTimes) {
        this.retrieveDataRetryTimes = retrieveDataRetryTimes;
    }


    public boolean isLocalFirst() {
        return localFirst;
    }


    public void setLocalFirst(boolean localFirst) {
        this.localFirst = localFirst;
    }


}
