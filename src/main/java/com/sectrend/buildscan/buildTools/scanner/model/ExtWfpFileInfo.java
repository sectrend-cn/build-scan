package com.sectrend.buildscan.buildTools.scanner.model;

import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/1/2 9:35
 **/
@Data
public class ExtWfpFileInfo {
    String fileHash;

    long fileSize;

    String filePath;

    String fileName;

    String rootName;

    String suffixName;

    String parentPath;

    List<String> parentPathNameList;

    boolean softwarePackage;

    int lineCount;

    boolean dirHashFlag;
}
