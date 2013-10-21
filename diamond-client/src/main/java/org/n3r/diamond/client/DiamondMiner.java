package org.n3r.diamond.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.client.impl.Constants;
import org.n3r.diamond.client.impl.DiamondSubstituter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiamondMiner {
    private static Logger log = LoggerFactory.getLogger(DiamondMiner.class);

    static Pattern bytesPattern = Pattern.compile("([\\d.]+)([GMK]B)", Pattern.CASE_INSENSITIVE);
    static Map<String, Integer> powerMap = ImmutableMap.of("GB", 3, "MB", 2, "KB", 1);

    public static long getBytes(String key) {
        return getBytes(Constants.DEFAULT_GROUP, key);
    }

    public static long getBytes(String group, String dataId) {
        String stone = getStone(group, dataId);

        long returnValue = -1;
        Matcher matcher = bytesPattern.matcher(stone);

        if (matcher.find()) {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase());
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }
        return returnValue;
    }

    public static JSONObject getJSON(String key) {
        String string = getString(key);
        if (string == null) return null;
        try {
            return JSON.parseObject(string);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static <T> T getJSON(String key, Class<T> clazz) {
        String string = getString(key);
        if (string == null) return null;
        try {
            return JSON.parseObject(string, clazz);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static JSONObject getJSON(String group, String dataId) {
        String string = getStone(group, dataId);
        if (string == null) return null;
        try {
            return JSON.parseObject(string);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static <T> T getJSON(String group, String dataId, Class<T> clazz) {
        String string = getStone(group, dataId);
        if (string == null) return null;
        try {
            return JSON.parseObject(string, clazz);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static JSONArray getJSONArray(String key) {
        String string = getString(key);
        if (string == null) return null;
        try {
            return JSON.parseArray(string);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static <T> List<T> getJSONArray(String key, Class<T> clazz) {
        String string = getString(key);
        if (string == null) return null;
        try {
            return JSON.parseArray(string, clazz);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static JSONArray getJSONArray(String group, String dataId) {
        String string = getStone(group, dataId);
        if (string == null) return null;
        try {
            return JSON.parseArray(string);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }

    public static <T> List<T> getJSONArray(String group, String dataId, Class<T> clazz) {
        String string = getStone(group, dataId);
        if (string == null) return null;
        try {
            return JSON.parseArray(string, clazz);
        } catch (Exception e) {
            log.error("parse string to JSON failed " + string, e);
            throw new DiamondException.WrongType(e);
        }
    }


    public static Properties getProperties(String key) {
        Properties properties = new Properties();
        String string = getString(key);
        if (string != null) {
            try {
                properties.load(new StringReader(string));
            } catch (IOException e) {
                log.error("load string to Properties failed " + string, e);
                throw new DiamondException.WrongType(e);
            }
        }
        return properties;
    }

    public static Properties getProperties(String group, String dataId) {
        Properties properties = new Properties();
        String string = getStone(group, dataId);
        if (string != null) {
            try {
                properties.load(new StringReader(string));
            } catch (IOException e) {
                // ignore
            }
        }
        return properties;
    }

    private static Pattern durationPattern = Pattern
            .compile("((\\d+)[dD])?\\s*((\\d+)[hH])?\\s*((\\d+)[mM])?\\s*((\\d+)[Ss])?");

    public static long getDuration(String duration, TimeUnit timeUnit) {
        Matcher m = durationPattern.matcher(duration);
        m.find();

        long miliSeconds = 0;

        TimeUnit millisecondsTimeUnit = TimeUnit.MILLISECONDS;
        if (StringUtils.isNotEmpty(m.group(2))) {
            int days = Integer.parseInt(m.group(2));
            miliSeconds += millisecondsTimeUnit.convert(days, TimeUnit.DAYS);
        }

        if (StringUtils.isNotEmpty(m.group(4))) {
            int hours = Integer.parseInt(m.group(4));
            miliSeconds += millisecondsTimeUnit.convert(hours, TimeUnit.HOURS);
        }

        if (StringUtils.isNotEmpty(m.group(6))) {
            int minutes = Integer.parseInt(m.group(6));
            miliSeconds += millisecondsTimeUnit.convert(minutes, TimeUnit.MINUTES);
        }

        if (StringUtils.isNotEmpty(m.group(8))) {
            int seconds = Integer.parseInt(m.group(8));
            miliSeconds += millisecondsTimeUnit.convert(seconds, TimeUnit.SECONDS);
        }

        return timeUnit.convert(miliSeconds, millisecondsTimeUnit);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCache(String key) {
        return (T) new DiamondManager(key).getCache();
    }

    @SuppressWarnings("unchecked")
    public static <T> T getCache(String group, String dataId) {
        return (T) new DiamondManager(group, dataId).getCache();
    }

    public static String getString(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return null;

        return DiamondSubstituter.substitute(diamond, true);
    }

    public static String getString(String key, String defaultValue) {
        return Objects.firstNonNull(getString(key), defaultValue);
    }

    public static String getStone(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return null;

        return DiamondSubstituter.substitute(diamond, true);
    }

    public static String getStone(String group, String dataId, String defaultValue) {
        return Objects.firstNonNull(getStone(group, dataId), defaultValue);
    }

    public static boolean exists(String group, String dataId) {
        return new DiamondManager(group, dataId).getDiamond() != null;
    }

    public static boolean exists(String key) {
        return new DiamondManager(key).getDiamond() != null;
    }

    public static int getInt(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Integer.parseInt(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static int getInt(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Integer.parseInt(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static int getInt(String key, int defaultValue) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Integer.parseInt(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static int getInt(String group, String dataId, int defaultValue) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Integer.parseInt(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static long getLong(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Long.parseLong(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static long getLong(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Long.parseLong(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static long getLong(String key, long defaultValue) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Long.parseLong(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static long getLong(String group, String dataId, long defaultValue) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Long.parseLong(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    private static boolean toBool(String str) {
        return "true".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str)
                || "on".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str);
    }

    public static boolean getBoolean(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        return toBool(diamond);
    }

    public static boolean getBoolean(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        return toBool(diamond);
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return defaultValue;
        return toBool(diamond);
    }

    public static boolean getBoolean(String group, String dataId, boolean defaultValue) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return defaultValue;
        return toBool(diamond);
    }


    public static float getFloat(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Float.parseFloat(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static float getFloat(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Float.parseFloat(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static float getFloat(String key, float defaultValue) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Float.parseFloat(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static float getFloat(String group, String dataId, float defaultValue) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Float.parseFloat(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static double getDouble(String key) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Double.parseDouble(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static double getDouble(String group, String dataId) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) throw new DiamondException.Missing();
        try {
            return Double.parseDouble(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static double getDouble(String key, double defaultValue) {
        String diamond = new DiamondManager(key).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Double.parseDouble(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

    public static double getDouble(String group, String dataId, double defaultValue) {
        String diamond = new DiamondManager(group, dataId).getDiamond();
        if (diamond == null) return defaultValue;
        try {
            return Double.parseDouble(diamond);
        } catch (NumberFormatException e) {
            throw new DiamondException.WrongType(e);
        }
    }

}
