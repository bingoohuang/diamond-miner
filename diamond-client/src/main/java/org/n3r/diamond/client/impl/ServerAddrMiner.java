package org.n3r.diamond.client.impl;

import com.google.common.net.HostAndPort;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ServerAddrMiner {
    private Logger log = LoggerFactory.getLogger(ServerAddrMiner.class);

    private volatile boolean running;
    private volatile DiamondManagerConf diamondManagerConf;

    private HttpClient httpClient;
    private SimpleHttpConnectionManager connectionManager;

    private ScheduledExecutorService scheduledExecutor;
    private int asynAcquireIntervalInSec = 300;

    public ServerAddrMiner(DiamondManagerConf diamondManagerConf, ScheduledExecutorService scheduledExecutor) {
        this.diamondManagerConf = diamondManagerConf;
        this.scheduledExecutor = scheduledExecutor;
    }

    public synchronized void start() {
        if (running) return;
        running = true;

        if (MockDiamondServer.isTestMode()) {
            diamondManagerConf.addDomainName("测试模式，没有使用的真实服务器");
            return;
        }

        initHttpClient();
        if (diamondManagerConf.isLocalFirst()) {
            acquireServerAddrFromLocal();
        } else {
            synAcquireServerAddress();
            asynAcquireServerAddress();
        }
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;

        connectionManager.shutdown();
    }

    private void initHttpClient() {
        connectionManager = new SimpleHttpConnectionManager();
        connectionManager.closeIdleConnections(5000L);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondManagerConf.isConnectionStaleCheckingEnabled());
        params.setConnectionTimeout(diamondManagerConf.getConnectionTimeout());
        connectionManager.setParams(params);

        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(new HostConfiguration());
    }

    protected void acquireServerAddrFromLocal() {
        reloadServerAddresses();

        if (diamondManagerConf.hasDiamondServers()) return;

        if (acquireServerAddr()) {
            log.info("在同步获取服务器列表时，获取到了服务器列表");
            saveServerAddrToLocal();
        }

        throw new RuntimeException("当前没有可用的服务器列表");
    }

    protected void synAcquireServerAddress() {
        if (acquireServerAddr()) {
            saveServerAddrToLocal();
            return;
        }

        reloadServerAddresses();
        if (!diamondManagerConf.hasDiamondServers())
            throw new RuntimeException("当前没有可用的服务器列表");
    }

    protected void asynAcquireServerAddress() {
        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                schedule();
            }
        }, asynAcquireIntervalInSec, asynAcquireIntervalInSec, TimeUnit.SECONDS);
    }

    private void schedule() {
        if (acquireServerAddr()) saveServerAddrToLocal();  // 存入本地文件
    }

    void saveServerAddrToLocal() {
        List<String> domainNameList = new ArrayList<String>(diamondManagerConf.getDomainNames());
        try {
            FileUtils.writeLines(generateLocalFile(), domainNameList);
        } catch (Exception e) {
            log.error("存储服务器地址到本地文件失败:{}", e.getMessage());
        }
    }

    private void reloadServerAddresses() {
        log.info("从本地获取Diamond地址列表");
        try {
            File serverAddressFile = generateLocalFile();
            if (!serverAddressFile.exists()) return;

            List<String> addresses = FileUtils.readLines(serverAddressFile);
            for (String address : addresses) {
                address = address.trim();
                if (StringUtils.isNotEmpty(address))
                    diamondManagerConf.getDomainNames().add(address);
            }

            if (diamondManagerConf.getDomainNames().size() > 0)
                log.info("在同步获取服务器列表时，本地指定了服务器列表，不进行同步");
        } catch (Exception e) {
            log.error("从本地文件取服务器地址失败", e);
        }
    }

    private File generateLocalFile() {
        String directory = diamondManagerConf.getFilePath();

        return new File(FilenameUtils.concat(directory, Constants.SERVER_ADDRESS));
    }

    /**
     * 获取diamond服务器地址列表
     */
    private boolean acquireServerAddr() {
        setHttpHostConfig();

        HttpMethod httpMethod = new GetMethod(Constants.DIAMOND_HTTP_URI);
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout(diamondManagerConf.getOnceTimeout());
        httpMethod.setParams(params);

        try {
            if (Constants.SC_OK != httpClient.executeMethod(httpMethod)) {
                log.warn("没有可用的新服务器列表");
                return false;
            }

            List<String> newDomainNameList = IOUtils.readLines(httpMethod.getResponseBodyAsStream());
            if (newDomainNameList.size() > 0) {
                log.info("在同步获取服务器列表时，向NameServer服务器获取到了服务器列表");
                diamondManagerConf.setDomainNames(newDomainNameList);
                return true;
            }
        } catch (Exception e) {
            log.error("从{}获取钻石挖掘服务器地址列表失败:{}",
                    httpClient.getHostConfiguration().getHost(), e.getMessage());
        } finally {
            httpMethod.releaseConnection();
        }
        return false;
    }

    private void setHttpHostConfig() {
        String nameServerAddr = getNameServerFromClasspath();
        if (null != nameServerAddr) {
            HostAndPort hostAndPort = HostAndPort.fromString(nameServerAddr);
            String configServerAddress = hostAndPort.getHostText();
            int port = hostAndPort.getPortOrDefault(Constants.DEF_DIAMOND_NAMESERVER_PORT);
            httpClient.getHostConfiguration().setHost(configServerAddress, port);
            return;
        }

        httpClient.getHostConfiguration().setHost(
                Constants.DEF_DOMAINNAME, Constants.DEF_DIAMOND_NAMESERVER_PORT);


    }

    private String getNameServerFromClasspath() {
        String nameServerAddr = null;
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(Constants.SERVER_ADDRESS);
            nameServerAddr = IOUtils.toString(is);
        } catch (IOException e) {
            log.error("fail to read classpath server address file " + Constants.SERVER_ADDRESS);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return nameServerAddr;
    }
}
