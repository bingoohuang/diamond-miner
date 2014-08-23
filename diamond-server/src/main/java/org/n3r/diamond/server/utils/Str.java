package org.n3r.diamond.server.utils;

public class Str {

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

}
