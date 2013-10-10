package org.n3r.diamond.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class Utils {
    private static Logger log = LoggerFactory.getLogger(Utils.class);

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

        InputStream is = getClassPathResourceAsStream(pathname);
        if (is != null || silent) return null;

        throw new RuntimeException("fail to find " + pathname + " in current dir or classpath");
    }

    public static InputStream getClassPathResourceAsStream(String resourceName) {
        return Utils.class.getClassLoader().getResourceAsStream(resourceName);
    }

    private static final String LOCAL_IP = getLocalHostAddress();

    public static String getLocalIp() {
        return LOCAL_IP;
    }

    private static String getLocalHostAddress() {
        String address = "127.0.0.1";
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface ni = en.nextElement();
                Enumeration<InetAddress> ads = ni.getInetAddresses();
                while (ads.hasMoreElements()) {
                    InetAddress ip = ads.nextElement();
                    if (!ip.isLoopbackAddress() && ip.isSiteLocalAddress()) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
        }
        return address;
    }


}
