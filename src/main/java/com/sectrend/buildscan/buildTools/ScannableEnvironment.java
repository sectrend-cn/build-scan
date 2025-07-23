package com.sectrend.buildscan.buildTools;


import com.sectrend.buildscan.enums.BuildScanTypeEnum;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.List;
import java.util.Properties;

@Getter
@Setter
public class ScannableEnvironment {

    /**
     * 包管理器构建业务参数
     */
    private Properties arguments;
    /**
     * 构建文件根目录
     */
    private final File directory;

    /**
     * 构建类型
     */
    private String buildType;

    /**
     * 项目多模块情况下会有多个依赖文件 (在无构建情况下需要循环遍历解析依赖文件)
     */
    private List<ScannableEnvironment> scannableEnvironmentList;

    /**
     * 依赖文件路径（依赖树解析用到）
     */
    private String buildTreeFile;

    /**
     * 引用文件
     */
    private List<File> referenceFileList;


    private String npmExclude;

    private BuildScanTypeEnum buildScanType = BuildScanTypeEnum.ALL;

    private boolean useGradlewFirst = false;

    public ScannableEnvironment(File directory) {
        this.directory = directory;
    }

    public ScannableEnvironment(File directory, String buildType, List<ScannableEnvironment> scannableEnvironmentList) {
        this.directory = directory;
        this.buildType = buildType;
        this.scannableEnvironmentList = scannableEnvironmentList;
    }

    public ScannableEnvironment(File directory, String buildTreeFile) {
        this.directory = directory;
        this.buildTreeFile = buildTreeFile;
    }

}
