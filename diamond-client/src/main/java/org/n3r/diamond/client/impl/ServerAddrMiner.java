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
            diamondManagerConf.addDomainName("Testing mode");
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
            log.info("sync succussfully to get diamond servers");
            saveServerAddrToLocal();
        }

        throw new RuntimeException("no diamond servers available");
    }

    protected void synAcquireServerAddress() {
        if (acquireServerAddr()) {
            saveServerAddrToLocal();
            return;
        }

        if (readClassPathServerAddress()) return;
        if (reloadServerAddresses()) return;

        throw new RuntimeException("no diamond servers available");
    }

    private boolean readClassPathServerAddress() {
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(Constants.SERVER_ADDRESS);
            List<String> serverAddress = IOUtils.readLines(is);
            log.info("got diamond servers from classpath {}", Constants.SERVER_ADDRESS);
            diamondManagerConf.setNameServers(serverAddress);
            return true;
        } catch (IOException e) {
            log.error("fail to read classpath server address file " + Constants.SERVER_ADDRESS);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return false;

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
        List<String> domainNameList = new ArrayList<String>(diamondManagerConf.getNameServers());
        try {
            FileUtils.writeLines(generateLocalFile(), domainNameList);
        } catch (Exception e) {
            log.error("save diamond servers to local failed ", e.getMessage());
        }
    }

    private boolean reloadServerAddresses() {
        log.info("read diamaond server addresses from local");
        try {
            File serverAddressFile = generateLocalFile();
            if (!serverAddressFile.exists()) return false;

            List<String> addresses = FileUtils.readLines(serverAddressFile);
            for (String address : addresses) {
                address = address.trim();
                if (StringUtils.isNotEmpty(address))
                    diamondManagerConf.getNameServers().add(address);
            }

            if (diamondManagerConf.getNameServers().size() > 0) {
                log.info("successfully to read diamaond server addresses from local");
                return true;
            }
        } catch (Exception e) {
            log.error("failed to read diamaond server addresses from local", e);
        }
        return false;
    }

    private File generateLocalFile() {
        String directory = diamondManagerConf.getFilePath();

        return new File(FilenameUtils.concat(directory, Constants.SERVER_ADDRESS));
    }

    private boolean acquireServerAddr() {
        setHttpHostConfig();

        HttpMethod httpMethod = new GetMethod(Constants.DIAMOND_HTTP_URI);
        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout(diamondManagerConf.getOnceTimeout());
        httpMethod.setParams(params);

        try {
            if (Constants.SC_OK != httpClient.executeMethod(httpMethod)) {
                log.warn("no diamond servers available\");");
                return false;
            }

            List<String> newDomainNameList = IOUtils.readLines(httpMethod.getResponseBodyAsStream());
            if (newDomainNameList.size() > 0) {
                log.info("got diamond servers from NameServer");
                diamondManagerConf.setNameServers(newDomainNameList);
                return true;
            }
        } catch (Exception e) {
            log.error("failed to get diamond servers from {} by {}",
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
            is = getClass().getClassLoader().getResourceAsStream(Constants.NAME_SERVER_ADDRESS);
            nameServerAddr = IOUtils.toString(is);
        } catch (IOException e) {
            log.error("fail to read classpath name server address file " + Constants.NAME_SERVER_ADDRESS);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return nameServerAddr;
    }
}
