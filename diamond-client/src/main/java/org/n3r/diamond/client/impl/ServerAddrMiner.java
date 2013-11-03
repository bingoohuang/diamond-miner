package org.n3r.diamond.client.impl;

import com.google.common.net.HostAndPort;
import org.apache.commons.httpclient.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.n3r.diamond.client.impl.ClientProperties.*;

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
        synAcquireServerAddress();
        asynAcquireServerAddress();
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;

        if (readNameServerMode() != NameServerMode.Off) connectionManager.shutdown();
    }

    private void initHttpClient() {
        if (readNameServerMode() != NameServerMode.Off) return;

        connectionManager = new SimpleHttpConnectionManager();
        connectionManager.closeIdleConnections(5000L);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondManagerConf.isConnectionStaleCheckingEnabled());
        params.setConnectionTimeout(diamondManagerConf.getConnectionTimeout());
        connectionManager.setParams(params);

        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(new HostConfiguration());

        // Disable retry
        HttpMethodRetryHandler retryHandler = new DefaultHttpMethodRetryHandler(0, false);
        httpClient.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, retryHandler);
    }

    protected void synAcquireServerAddress() {
        if (readNameServerMode() != NameServerMode.Off && acquireServerAddr()) {
            saveServerAddrToLocal();
            return;
        }

        if (readClientServerAddress()) return;
        if (reloadServerAddresses()) return;

        throw new RuntimeException("no diamond servers available");
    }

    private boolean readClientServerAddress() {
        List<String> serverAddress = readDiamondServersAddress();
        if (serverAddress.size() > 0) {
            diamondManagerConf.setNameServers(serverAddress);
            return true;
        }

        return false;
    }

    protected void asynAcquireServerAddress() {
        if (readNameServerMode() == NameServerMode.Off) return;

        scheduledExecutor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                if (acquireServerAddr()) saveServerAddrToLocal();
            }
        }, asynAcquireIntervalInSec, asynAcquireIntervalInSec, TimeUnit.SECONDS);
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
        HostAndPort hostAndPort = readNameServerAddress();
        if (hostAndPort == null) {
            log.error("no name server specified");
            return false;
        }

        httpClient.getHostConfiguration().setHost(hostAndPort.getHostText(), hostAndPort.getPort());

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

}
