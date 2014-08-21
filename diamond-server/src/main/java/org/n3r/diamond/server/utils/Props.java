package org.n3r.diamond.server.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

import static java.io.File.separator;

public class Props {
    static Logger log = LoggerFactory.getLogger(Props.class);

    public static Properties tryProperties(String pathname, String appHome) {
        return tryProperties(pathname, appHome, Silent.OFF);
    }

    public static Properties tryProperties(String pathname, String appHome, Silent silent) {
        Properties properties = new Properties();
        InputStream is = null;
        try {
            is = Props.tryResource(pathname, appHome, silent);
            if (is != null) properties.load(is);
        } catch (IOException e) {
            log.error("load properties error: {}", e.getMessage());
        } finally {
            IOUtils.closeQuietly(is);
        }

        return properties;
    }


    static enum Silent {ON, OFF}

    public static InputStream tryResource(String pathname, String appHome, Silent silent) {
        InputStream is = currentDirResource(new File(pathname));
        if (is != null) return is;

        is = userHomeResource(pathname, appHome);
        if (is != null) return is;

        is = classpathResource(pathname);
        if (is != null || silent == Silent.ON) return is;

        throw new RuntimeException("fail to find " + pathname + " in current dir or classpath");
    }

    private static InputStream userHomeResource(String pathname, String appHome) {
        String filePath = System.getProperty("user.home") + separator + appHome;
        File dir = new File(filePath);
        if (!dir.exists()) return null;

        return currentDirResource(new File(dir, pathname));
    }

    private static InputStream currentDirResource(File file) {
        if (!file.exists()) return null;

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            // This should not happened
            log.error("read file {} error", file, e);
            return null;
        }
    }

    public static InputStream classpathResource(String resourceName) {
        return Props.class.getClassLoader().getResourceAsStream(resourceName);
    }

}
