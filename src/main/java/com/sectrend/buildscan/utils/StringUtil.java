package com.sectrend.buildscan.utils;


/**
 * 字符串工具类
 *
 * @ClassName StringUtil
 * @Author Jimmy
 * @Date 1/20/25 17:42
 */
public class StringUtil {

    public static String getStrBeforeUpperCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        int index = -1;
        for (int i = 0; i < str.length(); i++) {
            if (Character.isUpperCase(str.charAt(i))) {
                index = i;
                break;
            }
        }
        return index != -1 ? str.substring(0, index) : str;
    }


}
