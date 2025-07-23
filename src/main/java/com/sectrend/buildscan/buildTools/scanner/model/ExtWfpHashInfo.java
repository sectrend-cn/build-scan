package com.sectrend.buildscan.buildTools.scanner.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/1/2 9:32
 **/
@Data
public class ExtWfpHashInfo {
    ExtWfpLineInfo lineInfo = new ExtWfpLineInfo();

    CopyOnWriteArraySet<String> rootNameList = new CopyOnWriteArraySet<>();

    ConcurrentHashMap<String, String> fileHashMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, List<String>> hashFileMap = new ConcurrentHashMap<>();

    ConcurrentHashMap<String, List<String>> dirChildHashMap = new ConcurrentHashMap<>();

    // 源码匹配中需要过滤的路径
    List<String> filterSourceCodeScanList = new ArrayList<>();

    // 需要进行二进制扫描的文件路径
    List<String> binaryScanList = new ArrayList<>();
}
