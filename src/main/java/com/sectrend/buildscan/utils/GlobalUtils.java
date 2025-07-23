package com.sectrend.buildscan.utils;

import org.apache.commons.lang3.StringUtils;


public class GlobalUtils {


    /**
     * 替换字符串最后一个指定字符
     * @param str 字符串
     * @param strToReplace  指定替换字符串
     * @param replaceWithThis  替换后字符串
     * @return
     */
    public static String replaceLast(String str, String strToReplace, String replaceWithThis) {
        if(StringUtils.isBlank(str))
            return null;
        return str.replaceFirst("(?s)" + strToReplace + "(?!.*?" + strToReplace + ")", replaceWithThis);
    }

}
