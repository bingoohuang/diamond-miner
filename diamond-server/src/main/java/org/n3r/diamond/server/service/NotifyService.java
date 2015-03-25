package org.n3r.diamond.server.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import org.apache.commons.io.IOUtils;
import org.n3r.diamond.server.domain.DiamondStone;
import org.n3r.diamond.server.utils.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Service
public class NotifyService {
    private Logger log = LoggerFactory.getLogger(NotifyService.class);

    private static final int TIMEOUT = 5000;
    private static final String URL_PREFIX = "/diamond-server/notify.do";
    private static final String PROTOCOL = "http://";

    private List<String> servers;

    @Autowired
    private DiamondService diamondService;

    @PostConstruct
    public void loadNodes() {
        // eg:127.0.0.1:17002 127.0.0.2:17002
        DiamondStone info = diamondService.findConfigInfo("servers", AdminService.ADMIN_GROUP);
        if (info != null) {
            servers = Splitter.onPattern("\\s+").trimResults()
                    .omitEmptyStrings().splitToList(info.getContent());
        } else {
            servers = Lists.newArrayList();
        }

        log.info("diamond-server nodes {}", servers.toString());
    }

    public void notifyConfigInfoChange(String dataId, String group) {
        for (String server : servers) {
            String urlString = generateNotifyConfigInfoPath(dataId, group, server);
            final String result = invokeURL(urlString);
            log.info("Notify {} config changed {}", server, result);
        }
    }

    String generateNotifyConfigInfoPath(String dataId, String group, String server) {
        HostAndPort hostAndPort = HostAndPort.fromString(server);

        String urlString = PROTOCOL + hostAndPort.getHostText() + ":"
                + hostAndPort.getPortOrDefault(Constants.DEF_DIAMOND_NODESERVER_PORT)
                + URL_PREFIX;

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
