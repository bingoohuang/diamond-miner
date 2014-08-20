package org.n3r.diamond.server.utils;

import com.alibaba.fastjson.JSON;
import org.n3r.diamond.server.security.Pbe;
import org.n3r.diamond.server.service.ServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.io.File.separator;

public class DiamondServerUtils {
    private static Logger log = LoggerFactory.getLogger(DiamondServerUtils.class);

    public static String processJson(HttpServletRequest request, ModelMap modelMap, Object page) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.indexOf("application/json") >= 0) {
            try {
                modelMap.addAttribute("pageJson", JSON.toJSONString(page));
            } catch (Exception e) {
                log.error("Json serialize page error", e);
            }

            return "/admin/config/list_json";
        }

        return null;
    }

    public static StringBuilder removeLastLetters(String s, char letter) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.charAt(sb.length() - 1) == letter)
            sb.deleteCharAt(sb.length() - 1);

        return sb;
    }

    public static StringBuilder padding(String s, char letter, int repeats) {
        StringBuilder sb = new StringBuilder(s);
        while (repeats-- > 0) {
            sb.append(letter);
        }

        return sb;
    }

    public static String purifyBase64(String s) {
        return removeLastLetters(s, '=').toString();
    }

    public static String paddingBase64(String s) {
        return padding(s, '=', s.length() % 4).toString();
    }

    public static String tryToEncryptContent(boolean encrypt, String dataId, String content) {
        Properties properties = tryEncryptProperties(content);
        if (properties != null) return getPropertyAsString(properties);

        return encrypt ? encryptValueWithKey(content, dataId) : content;
    }

    public static Properties tryEncryptProperties(String content) {
        CommentedProperties properties = new CommentedProperties();
        try {
            properties.load(new StringReader(content));
        } catch (IOException e) {
            // ignore
        }

        if (properties.size() == 0) return null;

        boolean hasRequiredEncryptValue = false;

        for (String key : properties.stringPropertyNames()) {
            String property = properties.getProperty(key);
            if (property.startsWith("#")) {
                String origin = property.substring(1);
                property = encryptValueWithKey(origin, key);
                properties.setProperty(key, property);

                hasRequiredEncryptValue = true;
            }

        }

        return hasRequiredEncryptValue ? properties : null;
    }

    private static String encryptValueWithKey(String origin, String key) {
        return "{PBE}" + purifyBase64(Pbe.encrypt(origin, key));
    }

    public static String getPropertyAsString(Properties prop) {
        StringWriter writer = new StringWriter();
        try {
            prop.store(writer, "");
        } catch (IOException e) {
        }
        return writer.getBuffer().toString();
    }


    static Pattern encryptPattern = Pattern.compile("\\{(...)\\}");

    public static String tryDecrypt(String original, String dataId) {
        if (original == null) return null;

        Matcher matcher = encryptPattern.matcher(original);
        if (!matcher.find() || matcher.start() != 0) return original;

        String encrypted = original.substring(5);
        String algrithm = matcher.group(1);
        if ("PBE".equalsIgnoreCase(algrithm)) {
            return Pbe.decrypt(paddingBase64(encrypted), dataId);
        }

        throw new RuntimeException(algrithm + " is not supported now");
    }

    public static Properties tryDecrypt(Properties properties) {
        Properties newProperties = new Properties();

        for (String key : properties.stringPropertyNames()) {
            String property = properties.getProperty(key);
            newProperties.put(key, tryDecrypt(property, key));
        }

        return newProperties;
    }


    public static InputStream toInputStreamFromCdOrClasspath(ServerProperties serverProperties, String pathname, boolean silent) {
        InputStream is = readFileFromCurrentDir(new File(pathname));
        if (is != null) return is;

        is = readFileFromDiamondServerHome(serverProperties, pathname);
        if (is != null) return is;

        is = getClassPathResourceAsStream(pathname);
        if (is != null || silent) return is;

        throw new RuntimeException("fail to find " + pathname + " in current dir or classpath");
    }

    private static InputStream readFileFromDiamondServerHome(ServerProperties serverProperties, String pathname) {
        if (serverProperties == null) return null;

        String filePath = System.getProperty("user.home") + separator + ".diamond-server" + serverProperties.getDumpPostfix();
        File dir = new File(filePath);
        if (!dir.exists()) return null;

        File file = new File(dir, pathname);

        return readFileFromCurrentDir(file);
    }

    private static InputStream readFileFromCurrentDir(File file) {
        if (!file.exists()) return null;
            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                // This should not happened
                log.error("read file {} error", file, e);
                return null;
            }
    }

    public static InputStream getClassPathResourceAsStream(String resourceName) {
        return DiamondServerUtils.class.getClassLoader().getResourceAsStream(resourceName);
    }
}
