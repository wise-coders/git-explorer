package com.wisecoders.util;


public class StringUtil {

    public static boolean isEmpty(Object obj) {
        return obj == null || obj.toString() == null || obj.toString().length() == 0;
    }

    public static boolean isFilled(Object obj) {
        return obj != null && obj.toString() != null && obj.toString().length() > 0;
    }

    public static boolean isFilledTrim(Object obj) {
        return obj != null && obj.toString() != null && obj.toString().trim().length() > 0;
    }

    public static boolean isEmptyTrim(String str) {
        return str == null || str.trim().length() == 0;
    }

}
