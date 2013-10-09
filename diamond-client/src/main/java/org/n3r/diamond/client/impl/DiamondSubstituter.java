package org.n3r.diamond.client.impl;

import org.apache.commons.lang3.StringUtils;
import org.n3r.diamond.client.DiamondMiner;

import java.util.HashSet;
import java.util.Set;


/**
 * Changed from spring PropertyPlaceholderConfigurer.java.
 * Support following formats:
 * 1) ${variable}
 * 2) ${variable:defaultValue}
 * 3) ${variabl${letter}} Recursive invocation
 * 4) mixed above
 *
 * @author Bingoo Huang
 */
public abstract class DiamondSubstituter {
    /**
     * Default Holder prefix: "${" .
     */
    public static final String DEF_HOLDER_PREFIX = "${";
    /**
     * Default Holder suffix: "}".
     */
    public static final String DEF_HOLDER_SUFFIX = "}";
    /**
     * Check system properties if not resolvable in the specified properties.
     * This is the default.
     */
    public static final int SYS_PROPS_MODE_FALLBACK = 1;

    /**
     * Check system properties first, before trying the specified properties.
     * This allows system properties to override any other property source.
     */
    public static final int SYS_PROPS_MODE_OVERRIDE = 2;

    private static final int DEF_HOLDER_PREFIX_LEN = DEF_HOLDER_PREFIX.length();
    private static final int DEF_HOLDER_SUFFIX_LEN = DEF_HOLDER_SUFFIX.length();



    /**
     * Parse the given String value recursively, to be able to resolve
     * nested Holders (when resolved property values in turn contain
     * Holders again).
     *
     */
    public static String substitute(String strVal, boolean ignoreBadHolders) {
        Set<String> visitedHolders = new HashSet<String>();
        return substitute(strVal, visitedHolders, ignoreBadHolders);
    }

    /**
     * Parse the given String value recursively, to be able to resolve
     * nested Holders (when resolved property values in turn contain
     * Holders again).
     *
     * @param strVal           the String value to parse
     * @param visitedHolders   the Holders that have already been visited
     * @param ignoreBadHolders during the current resolution attempt (used to detect circular references
     *                         between Holders). Only non-null if we're parsing a nested Holder.
     */
    public static String substitute(String strVal, Set<String> visitedHolders,
                                    boolean ignoreBadHolders) {

        StringBuffer buf = new StringBuffer(strVal);
        int startIndex = strVal.indexOf(DEF_HOLDER_PREFIX);
        while (startIndex != -1) {
            int endIndex = findHolderEndIndex(buf, startIndex);
            if (endIndex != -1) {
                String holder = buf.substring(startIndex + DEF_HOLDER_PREFIX_LEN, endIndex);
                String defValue = null;
                int defIndex = StringUtils.lastIndexOf(holder, ":");
                if (defIndex >= 0) {
                    defValue = StringUtils.trim(holder.substring(defIndex + 1));
                    holder = StringUtils.trim(holder.substring(0, defIndex));
                }

                if (!visitedHolders.add(holder)) {
                    throw new RuntimeException("Circular PlaceHolder reference '"
                            + holder + "' in property definitions");
                }
                // Recursive invocation, parsing Holders contained in the Holder key.
                holder = substitute(holder, visitedHolders, ignoreBadHolders);
                // Now obtain the value for the fully resolved key...
                String propVal = resolveHolder(holder, SYS_PROPS_MODE_FALLBACK, defValue);
                if (propVal != null) {
                    // Recursive invocation, parsing Holders contained in the
                    // previously resolved Holder value.
                    propVal = substitute(propVal, visitedHolders, ignoreBadHolders);
                    buf.replace(startIndex, endIndex + DEF_HOLDER_SUFFIX_LEN, propVal);

                    startIndex = buf.indexOf(DEF_HOLDER_PREFIX, startIndex + propVal.length());
                } else if (ignoreBadHolders) {
                    // Proceed with unprocessed value.
                    startIndex = buf.indexOf(DEF_HOLDER_PREFIX, endIndex + DEF_HOLDER_SUFFIX_LEN);
                } else {
                    throw new RuntimeException("Could not resolve Placeholder '" + holder + "'");
                }
                visitedHolders.remove(holder);
            } else {
                startIndex = -1;
            }
        }

        return buf.toString();
    }

    private static int findHolderEndIndex(CharSequence buf, int startIndex) {
        int index = startIndex + DEF_HOLDER_PREFIX_LEN;
        int withinNestedHolder = 0;
        while (index < buf.length()) {
            if (substringMatch(buf, index, DEF_HOLDER_SUFFIX)) {
                if (withinNestedHolder > 0) {
                    withinNestedHolder--;
                    index = index + DEF_HOLDER_SUFFIX_LEN;
                } else {
                    return index;
                }
            } else if (substringMatch(buf, index, DEF_HOLDER_PREFIX)) {
                withinNestedHolder++;
                index = index + DEF_HOLDER_PREFIX_LEN;
            } else {
                index++;
            }
        }
        return -1;
    }

    /**
     * Test whether the given string matches the given substring at the given index.
     *
     * @param str       the original string (or StringBuffer)
     * @param index     the index in the original string to start matching against
     * @param substring the substring to match at the given index
     * @return true/false
     */
    public static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
        for (int j = 0; j < substring.length(); j++) {
            int i = index + j;
            if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Resolve the given Holder using the given properties, performing
     * a system properties check according to the given mode.
     * <p>Default implementation delegates to <code>resolveHolder
     * (Holder, props)</code> before/after the system properties check.
     * <p>Subclasses can override this for custom resolution strategies,
     * including customized points for the system properties check.
     *
     * @param holder       the Holder to resolve
     * @param sysPropsMode the system properties mode,
     *                     according to the constants in this class
     * @return the resolved value, of null if none
     * @see System#getProperty
     */
    private static String resolveHolder(String holder, int sysPropsMode,
                                        String defaultValue) {
        String propVal = null;
        if (sysPropsMode == SYS_PROPS_MODE_OVERRIDE) {
            propVal = resolveSystemProperty(holder);
        }
        if (propVal == null) {
            propVal = resolveHolder(holder, defaultValue);
        }
        if (propVal == null && sysPropsMode == SYS_PROPS_MODE_FALLBACK) {
            propVal = resolveSystemProperty(holder);
        }
        return propVal;
    }

    /**
     * Resolve the given Holder using the given properties.
     * The default implementation simply checks for a corresponding property key.
     * <p>Subclasses can override this for customized Holder-to-key mappings
     * or custom resolution strategies, possibly just using the given properties
     * as fallback.
     * <p>Note that system properties will still be checked before respectively
     * after this method is invoked, according to the system properties mode.
     *
     * @param holder       the Holder to resolve
     * @return the resolved value, of <code>null</code> if none
     */
    protected static String resolveHolder(String holder, String defaultValue) {
        int seperate = holder.indexOf('/');
        String group = Constants.DEFAULT_GROUP;
        String dataId = holder;
        if (seperate > 0 && seperate < holder.length() - 1) {
            group = holder.substring(0, seperate);
            dataId = holder.substring(seperate + 1);
        }

        String value = DiamondMiner.getStone(group, dataId);
        if (value != null) return value;


        return defaultValue;
    }

    /**
     * Resolve the given key as JVM system property, and optionally also as
     * system environment variable if no matching system property has been found.
     *
     * @param key the Holder to resolve as system property key
     * @return the system property value, or <code>null</code> if not found
     * @see System#getProperty(String)
     * @see System#getenv(String)
     */
    private static String resolveSystemProperty(String key) {
        try {
            String value = System.getProperty(key);
            if (value == null) {
                value = System.getenv(key);
            }
            return value;
        } catch (Exception ex) {
            return null;
        }
    }

}
