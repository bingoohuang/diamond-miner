package org.n3r.diamond.server.service;

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

@Service
public class NotifyService {
    private Logger log = LoggerFactory.getLogger(NotifyService.class);

    private static final int TIMEOUT = 5000;
    private static final String URL_PREFIX = "/diamond-server/notify.do";
    private static final String PROTOCOL = "http://";

    private Properties nodeProps = new Properties();

    @Autowired
    private DiamondService diamondService;

    @PostConstruct
    public void loadNodes() {
        // eg:127.0.0.1:7001=server1 127.0.0.2:7001=server2 or 127.0.0.1:7001 127.0.0.2:7001
        DiamondStone info = diamondService.findConfigInfo("nameservers", "admin");
        if (info != null) {
            // \s+ 空格正则匹配
            List<String> lines = Splitter.onPattern("\\s+").trimResults().omitEmptyStrings().splitToList(info.getContent());
            for (String line : lines) {
                int equalPos = line.indexOf('=');
                if (equalPos < 0) {
                    nodeProps.put(line.trim(), "");
                } else {
                    String key = line.substring(0, equalPos);
                    String value = equalPos < line.length() - 1 ? line.substring(equalPos + 1) : "";
                    nodeProps.put(key.trim(), value.trim());
                }
            }
        }

        log.info("diamond-server nodes {}", nodeProps);
    }

    public void notifyConfigInfoChange(String dataId, String group) {
        Enumeration<?> enu = nodeProps.propertyNames();
        while (enu.hasMoreElements()) {
            String address = (String) enu.nextElement();
            // if (address.contains(LOCAL_IP)) continue;

            String urlString = generateNotifyConfigInfoPath(dataId, group, address);
            final String result = invokeURL(urlString);
            log.info("Notify {} config changed {}", address, result);
        }
    }

    String generateNotifyConfigInfoPath(String dataId, String group, String address) {
        String specialUrl = nodeProps.getProperty(address);

        HostAndPort hostAndPort = HostAndPort.fromString(address);

        String urlString = PROTOCOL + hostAndPort.getHostText() + ":"
                + hostAndPort.getPortOrDefault(Constants.DEF_DIAMOND_NAMESERVER_PORT)
                + URL_PREFIX;
        if (StringUtils.isNotBlank(specialUrl)) urlString = specialUrl;


        return urlString + "?method=notifyConfigInfo&dataId=" + dataId + "&group=" + group;
    }

    private String invokeURL(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestMethod("GET");
            conn.connect();
            return IOUtils.toString(conn.getInputStream());
        } catch (Exception e) {
            log.error("http invoke error,url=" + urlString, e.toString());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return "error";
    }
}
