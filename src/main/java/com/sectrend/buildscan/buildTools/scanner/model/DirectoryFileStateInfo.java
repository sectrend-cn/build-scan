package com.sectrend.buildscan.buildTools.scanner.model;

import lombok.Data;

/**
 * @Author huishun.yi
 * @Date 2025/2/17 16:53
 */
@Data
public class DirectoryFileStateInfo {

    // 目录下是否全部都是SO文件
    Integer isAllSoFile;

    // 目录下是否全部都是JAR文件
    Integer isAllJarFile;

}
