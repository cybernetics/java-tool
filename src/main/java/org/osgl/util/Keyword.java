package org.osgl.util;

import org.osgl.$;

import java.util.Arrays;

/**
 * A `Keyword` can be presented in the different ways:
 * * CamelCaseStyle
 * * underscore_style
 * * dash-style
 * * "readable style"
 *
 * When reading a string into a keyword, the following separator chars
 * will be ignored and used as separator to constructo the keyword
 *
 * * space `' '`
 * * underscore: `_`
 * * dash: `-`
 * * comma: `,`
 * * dot: `.`
 * * colon: `:`
 * * semi-colon: `;`
 * * slash: `\`
 * * forward slash: `/`
 *
 */
public final class Keyword implements Comparable<Keyword> {

    public static final char SEP_SPACE = ' ';
    public static final char SEP_UNDERSCORE = '_';
    public static final char SEP_DASH = '-';
    public static final char SEP_COMMA = ',';
    public static final char SEP_COLON = ':';
    public static final char SEP_DOT = '.';
    public static final char SEP_SEMI_COLON = ';';
    public static final char SEP_SLASH = '\\';
    public static final char SEP_FORWARD_SLASH = '/';

    private static final char[] SEPS = {
            SEP_SPACE, SEP_UNDERSCORE, SEP_DASH, SEP_COMMA,
            SEP_COLON, SEP_DOT, SEP_SEMI_COLON, SEP_FORWARD_SLASH,
            SEP_SLASH
    };

    static {
        Arrays.sort(SEPS);
    }

    public static enum Style {
        CAMEL_CASE () {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                return token.capFirst();
            }
        },
        UNDERSCORE(SEP_UNDERSCORE),
        DASHED(SEP_DASH),
        READABLE(SEP_SPACE) {
            @Override
            protected CharSequence processToken(FastStr token, int seq) {
                if (seq == 0) {
                    return token.capFirst();
                }
                return token;
            }
        };

        private String separator;

        private Style() {
            separator = null;
        }

        private Style(char sep) {
            separator = String.valueOf(sep);
        }

        public String toString(Keyword keyword) {
            StringBuilder sb = S.builder();
            int sz = keyword.list.size();
            for (int i = 0; i < sz; i++) {
                FastStr fs = keyword.list.get(i);
                sb.append(processToken(fs, i));
                if (i < sz - 1 && null != separator) {
                    sb.append(separator);
                }
            }
            return sb.toString();
        }

        protected CharSequence processToken(FastStr token, int seq) {
            return token;
        }
    }

    private C.List<FastStr> list = C.newList();

    public Keyword(CharSequence chars) {
        init(chars);
    }

    public String camelCase() {
        return Style.CAMEL_CASE.toString(this);
    }

    public String underscore() {
        return Style.UNDERSCORE.toString(this);
    }

    public String dashed() {
        return Style.DASHED.toString(this);
    }

    public String readable() {
        return Style.READABLE.toString(this);
    }


    @Override
    public int hashCode() {
        return $.hc(list);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Keyword) {
            return ((Keyword) obj).list.equals(list);
        }
        return false;
    }

    @Override
    public String toString() {
        return underscore();
    }

    @Override
    public int compareTo(Keyword o) {
        return camelCase().compareTo(o.camelCase());
    }

    public static Keyword of(CharSequence chars) {
        return new Keyword(chars);
    }

    private void init(CharSequence chars) {
        final FastStr fs = FastStr.of(chars);
        final int sz = fs.length();
        int last = nextNonSeparator(fs, 0);
        int pos;
        while (true) {
            pos = locateNextStop(fs, last);
            if (pos < 0 || pos == sz) {
                FastStr sub = fs.substr(last);
                if (!sub.isEmpty()) {
                    list.add(sub.toLowerCase());
                }
                break;
            }
            FastStr sub = fs.subSequence(last, pos);
            if (sub.length() == 1 && isUpperCase(sub.charAt(0))) {
                pos = nextNonUpperCase(fs, pos);
                sub = fs.subSequence(last, pos);
            }
            if (!sub.isEmpty()) {
                list.add(sub.toLowerCase());
            }
            last = nextNonSeparator(fs, pos);
        }
    }

    /*
     * next stop is at:
     * 1. Uppercase character that followed a non-uppercase character
     * 2. separator
     */
    private static int locateNextStop(FastStr str, int start) {
        int sz = str.length();
        if (start >= sz - 1) {
            return -1;
        }
        int pos = start + 1;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isSeparator(ch) || isUpperCase(ch)) {
                break;
            }
            pos++;
        }
        return pos;
    }

    private static int nextNonSeparator(FastStr str, int start) {
        int sz = str.length();
        int pos = start;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isSeparator(ch)) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    private static int nextNonUpperCase(FastStr str, int start) {
        final int sz = str.size();
        int pos = start;
        while (pos < sz) {
            char ch = str.charAt(pos);
            if (isUpperCase(ch)) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    private static boolean isSeparator(char ch) {
        return Arrays.binarySearch(SEPS, ch) >= 0;
    }

    private static boolean isUpperCase(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }


}