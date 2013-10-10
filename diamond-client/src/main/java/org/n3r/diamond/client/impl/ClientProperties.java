package org.n3r.diamond.client.impl;

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Properties;

public class ClientProperties {
    private static Logger log = LoggerFactory.getLogger(ClientProperties.class);

    private static Properties properties = new Properties();

    static {
        InputStream is = null;
        try {
            is = toInputStreamFromCdOrClasspath("diamond-client.properties", true);
            if (is != null) properties.load(is);
        } catch (IOException e) {
            log.error("load properties error", e);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static HostAndPort readNameServerAddress() {
        NameServerMode nameServerMode = readNameServerMode();
        switch (nameServerMode) {
            case ByAddressProperty:
                String nameServerAddress = properties.getProperty(Constants.NAME_SERVER_ADDRESS);
                if (StringUtils.isNotEmpty(nameServerAddress)) {
                    return HostAndPort.fromString(nameServerAddress).withDefaultPort(Constants.DEF_NAMESERVER_PORT);
                }
                log.warn("NameServerMode=ByAddressProperty without NameServer.address set");
                return null;
            case ByEtcHosts:
                return HostAndPort.fromParts(Constants.DEF_DOMAINNAME, Constants.DEF_NAMESERVER_PORT);

        }

        return null;
    }

    public static List<String> readDiamondServersAddress() {
        String diamondServersAddress = properties.getProperty(Constants.SERVER_ADDRESS, "");
        Splitter splitter = Splitter.onPattern("\\s+").omitEmptyStrings().trimResults();
        List<String> addresses = splitter.splitToList(diamondServersAddress);

        if (addresses.size() > 0)
            log.info("got diamond servers {} from config {}", addresses, Constants.SERVER_ADDRESS);

        return addresses;
    }

    public static InputStream toInputStreamFromCdOrClasspath(String pathname, boolean silent) {
        File diamondJdbc = new File(pathname);
        if (diamondJdbc.exists()) {
            try {
                return new FileInputStream(diamondJdbc);
            } catch (FileNotFoundException e) {
                // This should not happened
                log.error("read file {} error", pathname, e);
                return null;
            }
        }

        ClassLoader classLoader = ClientProperties.class.getClassLoader();
        InputStream is = classLoader.getResourceAsStream(pathname);
        if (is != null || silent) return is;

        throw new RuntimeException("fail to find " + pathname + " in current dir or classpath");
    }

    public static NameServerMode readNameServerMode() {
        String property = properties.getProperty("NameServerMode", "Off");
        try {
            return NameServerMode.valueOf(property);
        } catch (IllegalArgumentException e) {
            log.warn("NameServerMode's value is set to Off because of unknown config {}", property);
            return NameServerMode.Off;
        }
    }

    public static enum NameServerMode {
        Off, ByEtcHosts, ByAddressProperty
    }

}
