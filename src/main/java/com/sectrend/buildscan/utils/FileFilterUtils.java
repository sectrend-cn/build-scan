package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.enums.BuildType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

/**
 * 过滤文件夹
 */
public class FileFilterUtils {

    public static Map<String, List<String>> fileFilterRuleMap = new HashMap<>();

    static {

//        fileFilterRuleMap.put(BuildType.NPM_BUILD.getBuildType(), Collections.singletonList("node_modules"));
        fileFilterRuleMap.put(BuildType.MVN_BUILD.getBuildType(), Collections.singletonList("target"));
//        fileFilterRuleMap.put(BuildType.OHPM_BUILD_TYPE.getBuildType(), Collections.singletonList("oh_modules"));

    }

    /**
     * 筛选文件夹
     * @param file
     * @param buildTypeList
     * @return
     */
    public static boolean filterDirectory(File file, List<String> buildTypeList) {
        // .开头的文件不扫描
        if (Objects.isNull(file) || file.getName().startsWith(".")) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(buildTypeList)) {
            for (String buildType : buildTypeList) {
                // 判断当前构建类型是否有设置 过滤文件清单
                // 判断文件名称是否在文件过滤清单中, 如果在则 直接返回false
                if (fileFilterRuleMap.containsKey(buildType)
                        && fileFilterRuleMap.get(buildType).contains(file.getName())) {
                    return false;
                }
            }
        }
        return true;
    }


    public static boolean filterDirectory(String filePath, Set<String> buildTypes) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }
        if (CollectionUtils.isNotEmpty(buildTypes)) {
            for (String buildType : buildTypes) {
                List<String> dirs = fileFilterRuleMap.get(buildType);
                if (CollectionUtils.isEmpty(dirs)) {
                    continue;
                }
                boolean anyMatch = dirs.stream().anyMatch(dir -> filePath.contains(File.separator + dir + File.separator));
                if (anyMatch) {
                    return false;
                }
            }
        }
        return true;
    }

}
