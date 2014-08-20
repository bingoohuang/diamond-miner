package org.n3r.diamond.server.utils;


import java.io.*;
import java.util.Vector;

/**
 * The CommentedProperties class is an extension of java.util.Properties
 * <p/>
 * to allow retention of comment lines and blank (whitespace only) lines
 * <p/>
 * in the properties file.
 * <p/>
 * <p/>
 * <p/>
 * Written for Java version 1.4
 */
public class CommentedProperties extends java.util.Properties {
    /**
     * Use a Vector to keep a copy of lines that are a comment or 'blank'
     */
    public Vector lineData = new Vector(0, 1);

    /**
     * Use a Vector to keep a copy of lines containing a key, i.e. they are a property.
     */
    public Vector keyData = new Vector(0, 1);

    /**
     * Load properties from the specified InputStream.
     * <p/>
     * Overload the load method in Properties so we can keep comment and blank lines.
     *
     * @param reader The InputStream to read.
     */
    @Override
    public void load(Reader input) throws IOException {
        // The spec says that the file must be encoded using ISO-9-1.
        BufferedReader reader = new BufferedReader(input);
        String line;

        while ((line = reader.readLine()) != null) {
            char c = 0;
            int pos = 0;
            // Leading whitespaces must be deleted first.
            while (pos < line.length()
                    && Character.isWhitespace(c = line.charAt(pos))) {
                pos++;
            }

            // If empty line or begins with a comment character, save this line
            // in lineData and save a "" in keyData.
            if ((line.length() - pos) == 0 || line.charAt(pos) == '#' || line.charAt(pos) == '!') {
                lineData.add(line);
                keyData.add("");
                continue;
            }

            // The characters up to the next Whitespace, ':', or '='
            // describe the key.  But look for escape sequences.
            // Try to short-circuit when there is no escape char.
            int start = pos;
            boolean needsEscape = line.indexOf('\\', pos) != -1;
            StringBuffer key = needsEscape ? new StringBuffer() : null;

            while (pos < line.length()
                    && !Character.isWhitespace(c = line.charAt(pos++))
                    && c != '=' && c != ':') {
                if (needsEscape && c == '\\') {
                    if (pos == line.length()) {
                        // The line continues on the next line.  If there
                        // is no next line, just treat it as a key with an
                        // empty value.
                        line = reader.readLine();
                        if (line == null)
                            line = "";

                        pos = 0;
                        while (pos < line.length()
                                && Character.isWhitespace(c = line.charAt(pos)))
                            pos++;
                    } else {
                        c = line.charAt(pos++);
                        switch (c) {
                            case 'n':
                                key.append('\n');
                                break;
                            case 't':
                                key.append('\t');
                                break;
                            case 'r':
                                key.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt
                                            (line.substring(pos, pos + 4), 16);
                                    key.append(uni);
                                    pos += 4;
                                }   // else throw exception?
                                break;
                            default:
                                key.append(c);
                                break;
                        }
                    }
                } else if (needsEscape)
                    key.append(c);
            }

            boolean isDelim = (c == ':' || c == '=');

            String keyString;

            if (needsEscape)
                keyString = key.toString();
            else if (isDelim || Character.isWhitespace(c))
                keyString = line.substring(start, pos - 1);
            else
                keyString = line.substring(start, pos);

            while (pos < line.length()
                    && Character.isWhitespace(c = line.charAt(pos)))
                pos++;

            if (!isDelim && (c == ':' || c == '=')) {
                pos++;
                while (pos < line.length()
                        && Character.isWhitespace(c = line.charAt(pos)))
                    pos++;
            }

            // Short-circuit if no escape chars found.
            if (!needsEscape) {
                put(keyString, line.substring(pos));
                // Save a "" in lineData and save this
                // keyString in keyData.
                lineData.add("");
                keyData.add(keyString);
                continue;

            }

            // Escape char found so iterate through the rest of the line.
            StringBuffer element = new StringBuffer(line.length() - pos);
            while (pos < line.length()) {
                c = line.charAt(pos++);
                if (c == '\\') {
                    if (pos == line.length()) {
                        // The line continues on the next line.
                        line = reader.readLine();

                        // We might have seen a backslash at the end of
                        // the file.  The JDK ignores the backslash in
                        // this case, so we follow for compatibility.
                        if (line == null)
                            break;

                        pos = 0;
                        while (pos < line.length()
                                && Character.isWhitespace(c = line.charAt(pos)))
                            pos++;

                        element.ensureCapacity(line.length() - pos +
                                element.length());
                    } else {
                        c = line.charAt(pos++);
                        switch (c) {
                            case 'n':
                                element.append('\n');
                                break;
                            case 't':
                                element.append('\t');
                                break;
                            case 'r':
                                element.append('\r');
                                break;
                            case 'u':
                                if (pos + 4 <= line.length()) {
                                    char uni = (char) Integer.parseInt
                                            (line.substring(pos, pos + 4), 16);
                                    element.append(uni);
                                    pos += 4;
                                }   // else throw exception?
                                break;
                            default:
                                element.append(c);
                                break;
                        }
                    }
                } else
                    element.append(c);
            }

            put(keyString, element.toString());

            // Save a "" in lineData and save this
            // keyString in keyData.

            lineData.add("");
            keyData.add(keyString);
        }
    }

    /**
     * Write the properties to the specified OutputStream.
     * <p/>
     * <p/>
     * <p/>
     * Overloads the store method in Properties so we can put back comment
     * <p/>
     * and blank lines.
     *
     * @param out    The OutputStream to write to.
     * @param comments Ignored, here for compatability w/ Properties.
     * @throws IOException
     */
    @Override
    public void store(Writer out, String comments) throws IOException {
        // The spec says that the file must be encoded using ISO-9-1.
        PrintWriter writer = new PrintWriter(out);

        // We ignore the header, because if we prepend a commented header
        // then read it back in it is now a comment, which will be saved
        // and then when we write again we would prepend Another header...
        String line;
        String key;
        StringBuffer s = new StringBuffer();

        for (int i = 0; i < lineData.size(); i++) {
            line = (String) lineData.get(i);
            key = (String) keyData.get(i);
            if (key.length() > 0) {  // This is a 'property' line, so rebuild it
                formatForOutput(key, s, true);
                s.append('=');
                formatForOutput((String) get(key), s, false);
                writer.println(s);
            } else {  // was a blank or comment line, so just restore it
                writer.println(line);
            }
        }

        writer.flush();
        writer.close();
    }

    /**
     * Need this method from Properties because original code has StringBuilder,
     * <p/>
     * which is an element of Java 1.5, used StringBuffer instead (because
     * <p/>
     * this code was written for Java 1.4)
     *
     * @param str    - the string to format
     * @param buffer - buffer to hold the string
     * @param key    - true if str the key is formatted, false if the value is formatted
     */
    private void formatForOutput(String str, StringBuffer buffer, boolean key) {
        if (key) {
            buffer.setLength(0);
            buffer.ensureCapacity(str.length());
        } else
            buffer.ensureCapacity(buffer.length() + str.length());

        boolean head = true;
        int size = str.length();
        for (int i = 0; i < size; i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\t':
                    buffer.append("\\t");
                    break;
                case ' ':
                    buffer.append(head ? "\\ " : " ");
                    break;
                case '\\':
                case '!':
                case '#':
                case '=':
                case ':':
                    buffer.append('\\').append(c);
                    break;

                default:
                    if (c < ' ' || c > '~') {
                        String hex = Integer.toHexString(c);
                        buffer.append("\\u0".substring(0, 6 - hex.length()));
                        buffer.append(hex);
                    } else
                        buffer.append(c);
            }

            if (c != ' ')
                head = key;

        }
    }


    /**
     * Add a Property to the end of the CommentedProperties.
     *
     * @param keyString The Property key.
     * @param value     The value of this Property.
     */

    public void add(String keyString, String value) {
        put(keyString, value);
        lineData.add("");
        keyData.add(keyString);
    }


    /**
     * Add a comment or blank line or comment to the end of the CommentedProperties.
     *
     * @param line The string to add to the end, make sure this is a comment
     *             <p/>
     *             or a 'whitespace' line.
     */
    public void addLine(String line) {
        lineData.add(line);
        keyData.add("");
    }
}
