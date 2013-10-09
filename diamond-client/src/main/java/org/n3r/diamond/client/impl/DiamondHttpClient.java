package org.n3r.diamond.client.impl;

import com.google.common.net.HostAndPort;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

class DiamondHttpClient {
    private MultiThreadedHttpConnectionManager connectionManager;
    private Logger log = LoggerFactory.getLogger(DiamondHttpClient.class);
    private final DiamondManagerConf diamondManagerConf;
    private HttpClient httpClient;

    public DiamondHttpClient(DiamondManagerConf diamondManagerConf) {
        this.diamondManagerConf = diamondManagerConf;

        if (MockDiamondServer.isTestMode()) return;

        connectionManager = new MultiThreadedHttpConnectionManager();
        connectionManager.closeIdleConnections(diamondManagerConf.getPollingInterval() * 4000);

        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setStaleCheckingEnabled(diamondManagerConf.isConnectionStaleCheckingEnabled());
        HostConfiguration hostConfiguration = new HostConfiguration();
        params.setMaxConnectionsPerHost(hostConfiguration, diamondManagerConf.getMaxHostConnections());
        params.setMaxTotalConnections(diamondManagerConf.getMaxTotalConnections());
        params.setConnectionTimeout(diamondManagerConf.getConnectionTimeout());
        params.setSoTimeout(60 * 1000);  // socket waiting timeout 1 min

        connectionManager.setParams(params);
        httpClient = new HttpClient(connectionManager);
        httpClient.setHostConfiguration(hostConfiguration);
        setHostConfig();
    }

    private void setHostConfig() {
        String hostPort = diamondManagerConf.getDomainName();
        HostAndPort hostAndPort = HostAndPort.fromString(hostPort);
        int portOrDefault = hostAndPort.getPortOrDefault(Constants.DEFAULT_DIAMOND_SERVER_PORT);
        HostConfiguration hostConfiguration = httpClient.getHostConfiguration();
        hostConfiguration.setHost(hostAndPort.getHostText(), portOrDefault);
    }


    private void configureHttpMethod(HttpMethod httpMethod, boolean useContentCache,
                                     DiamondMeta diamondMeta, long onceTimeOut) {
        if (!useContentCache && null != diamondMeta) {
            String lastModifiedHeader = diamondMeta.getLastModifiedHeader();
            if (null != lastModifiedHeader && Constants.NULL != lastModifiedHeader) {
                httpMethod.addRequestHeader(Constants.IF_MODIFIED_SINCE, lastModifiedHeader);
            }
            if (null != diamondMeta.getMd5() && Constants.NULL != diamondMeta.getMd5()) {
                httpMethod.addRequestHeader(Constants.CONTENT_MD5, diamondMeta.getMd5());
            }
        }

        httpMethod.addRequestHeader(Constants.ACCEPT_ENCODING, "gzip,deflate");

        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout((int) onceTimeOut);
        httpMethod.setParams(params);

        setHostConfig();
    }

    public HttpState getState() {
        return httpClient.getState();
    }

    public GetDiamondResult getDiamond(String uri, boolean useContentCache,
                                       DiamondMeta diamondMeta, long onceTimeOut) throws IOException {
        GetMethod getMethod = new GetMethod(uri);
        try {
            configureHttpMethod(getMethod, useContentCache, diamondMeta, onceTimeOut);
            int httpStatus = httpClient.executeMethod(getMethod);

            GetDiamondResult getDiamondResult = new GetDiamondResult();
            getDiamondResult.setHttpStatus(httpStatus);

            if (httpStatus == Constants.SC_OK) {
                setResponseContent(getMethod, getDiamondResult);
                setLastModified(getMethod, getDiamondResult);
            }
            if (httpStatus == Constants.SC_OK || httpStatus == Constants.SC_NOT_MODIFIED) {
                setMd5(getMethod, getDiamondResult);
                setPollingInterval(getMethod, getDiamondResult);
            }

            return getDiamondResult;
        } finally {
            getMethod.releaseConnection();
        }
    }

    private void setResponseContent(GetMethod getMethod, GetDiamondResult getDiamondResult) {
        String responseContent = getContentFromResponse(getMethod);
        if (null == responseContent) throw new RuntimeException("RP_OK got bad info");
        getDiamondResult.setResponseContent(responseContent);
    }

    private void setLastModified(GetMethod getMethod, GetDiamondResult getDiamondResult) {
        Header lastModifiedHeader = getMethod.getResponseHeader(Constants.LAST_MODIFIED);
        if (null == lastModifiedHeader) throw new RuntimeException("RP_OK without lastModifiedHeader");

        String lastModified = lastModifiedHeader.getValue();
        getDiamondResult.setLastModified(lastModified);
    }

    private void setMd5(GetMethod getMethod, GetDiamondResult getDiamondResult) {
        Header md5Header = getMethod.getResponseHeader(Constants.CONTENT_MD5);
        if (null == md5Header) throw new RuntimeException("RP_NO_CHANGE without MD5");
        getDiamondResult.setMd5(md5Header.getValue());
    }

    private void setPollingInterval(GetMethod getMethod, GetDiamondResult getDiamondResult) {
        Header[] spacingIntervalHeaders = getMethod.getResponseHeaders(Constants.SPACING_INTERVAL);
        if (spacingIntervalHeaders.length >= 1) {
            try {
                getDiamondResult.setPollingInterval(Integer.parseInt(spacingIntervalHeaders[0].getValue()));
            } catch (RuntimeException e) {
                log.error("set polling interval error", e);
            }
        }
    }

    public CheckResult checkUpdateDataIds(String probeUpdateString, long onceTimeOut) throws IOException {
        PostMethod postMethod = new PostMethod(Constants.HTTP_URI_FILE);

        postMethod.addParameter(Constants.PROBE_MODIFY_REQUEST, probeUpdateString);

        HttpMethodParams params = new HttpMethodParams();
        params.setSoTimeout((int) onceTimeOut);
        postMethod.setParams(params);

        try {
            setHostConfig();
            int httpStatus = httpClient.executeMethod(postMethod);
            Set<String> updateDataIdsInBody = getUpdateDataIdsInBody(postMethod);
            return new CheckResult(httpStatus, updateDataIdsInBody);
        } finally {
            postMethod.releaseConnection();
        }
    }


    private Set<String> getUpdateDataIdsInBody(HttpMethod httpMethod) {
        try {
            String modifiedDataIdsString = httpMethod.getResponseBodyAsString();
            return DiamondUtils.convertStringToSet(modifiedDataIdsString);
        } catch (Exception e) {

        }
        return new HashSet<String>();
    }

    public void shutdown() {
        connectionManager.shutdown();
    }

    public static class GetDiamondResult {
        private int httpStatus;
        private String md5;
        private int pollingInterval;
        private String responseContent;
        private String lastModified;

        public void setHttpStatus(int httpStatus) {
            this.httpStatus = httpStatus;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public String getMd5() {
            return md5;
        }

        public void setPollingInterval(int pollingIntervalTime) {
            this.pollingInterval = pollingIntervalTime;
        }

        public int getPollingInterval() {
            return pollingInterval;
        }

        public void setResponseContent(String responseContent) {
            this.responseContent = responseContent;
        }

        public String getResponseContent() {
            return responseContent;
        }

        public void setLastModified(String lastModified) {
            this.lastModified = lastModified;
        }

        public String getLastModified() {
            return lastModified;
        }
    }


    private String getContentFromResponse(HttpMethod httpMethod) {
        if (isZipContent(httpMethod)) {
            InputStream is = null;
            GZIPInputStream gzin = null;
            try {
                is = httpMethod.getResponseBodyAsStream();
                gzin = new GZIPInputStream(is);
                return IOUtils.toString(gzin);
            } catch (Exception e) {
                log.error("ungzip error", e);
            } finally {
                IOUtils.closeQuietly(gzin);
                IOUtils.closeQuietly(is);
            }
        } else {
            try {
                return httpMethod.getResponseBodyAsString();
            } catch (Exception e) {
                log.error("getResponseBodyAsString error", e);
            }
        }

        return null;
    }


    private boolean isZipContent(HttpMethod httpMethod) {
        Header responseHeader = httpMethod.getResponseHeader(Constants.CONTENT_ENCODING);
        if (null == responseHeader) return false;

        String acceptEncoding = responseHeader.getValue();
        return acceptEncoding.toLowerCase().indexOf("gzip") > -1;
    }

    public static class CheckResult {
        private final int httpStatus;
        private final Set<String> updateDataIdsInBody;

        public CheckResult(int httpStatus, Set<String> updateDataIdsInBody) {
            this.httpStatus = httpStatus;
            this.updateDataIdsInBody = updateDataIdsInBody;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public Set<String> getUpdateDataIdsInBody() {
            return updateDataIdsInBody;
        }
    }
}
