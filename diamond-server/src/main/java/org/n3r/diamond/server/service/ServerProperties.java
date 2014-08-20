package org.n3r.diamond.server.service;

import org.apache.commons.io.IOUtils;
import org.n3r.diamond.server.utils.DiamondServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
public class ServerProperties {
    private Logger log = LoggerFactory.getLogger(ServerProperties.class);
    private Properties serverProperties = new Properties();

    @PostConstruct
    public void loadProperties() {
        String resourceName = "diamond-server.properties";
        InputStream is = DiamondServerUtils.toInputStreamFromCdOrClasspath(null, resourceName, true);
        try {
            if (is != null) serverProperties.load(is);
        } catch (IOException e) {
            log.error("load properties from " + resourceName + " fail", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public String getDumpPostfix() {
        return serverProperties.getProperty("dump.postfix", "");
    }
}
