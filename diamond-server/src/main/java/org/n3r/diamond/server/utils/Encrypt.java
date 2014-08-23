package org.n3r.diamond.server.utils;

import org.n3r.diamond.server.security.Pbe;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.n3r.diamond.server.utils.Str.paddingBase64;
import static org.n3r.diamond.server.utils.Str.purifyBase64;

public class Encrypt {

    public static String tryToEncryptContent(boolean encrypt, String dataId, String content) {
        Properties properties = tryEncryptProperties(content);
        if (properties != null) return Props.getPropertyAsString(properties);

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

    public static String encryptValueWithKey(String origin, String key) {
        return "{PBE}" + purifyBase64(Pbe.encrypt(origin, key));
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

    public static Properties encrypt(Properties properties) {
        Properties newProperties = new Properties();

        for (String key : properties.stringPropertyNames()) {
            String property = properties.getProperty(key);
            newProperties.put(key, encryptValueWithKey(property, key));
        }

        return newProperties;
    }
}
