package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.enums.BuildScanTypeEnum;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;

public interface ExtractHandler {

    /**
     *
     * @param scannableEnvironment 构建信息
     * @param taskDir 构建文件根目录
     * @param simpleExecutableResolver 包管理器执行文件
     */
    ScanResults handler(ScannableEnvironment scannableEnvironment,
                        String taskDir, SimpleExecutableResolver simpleExecutableResolver)
            throws Throwable;


    /**
     * 是否执行构建
     * @param buildScanType
     * @return
     */
    default boolean isExecuteBuild(BuildScanTypeEnum buildScanType) {
        return BuildScanTypeEnum.BUILD.getValue().equals(buildScanType.getValue())
                || BuildScanTypeEnum.ALL.getValue().equals(buildScanType.getValue());
    }

    /**
     * 是否执行非构建
     * @param buildScanType
     * @return
     */
    default boolean isExecuteNonBuild(BuildScanTypeEnum buildScanType) {
        return BuildScanTypeEnum.NON_BUILD.getValue().equals(buildScanType.getValue())
                || BuildScanTypeEnum.ALL.getValue().equals(buildScanType.getValue());
    }

}