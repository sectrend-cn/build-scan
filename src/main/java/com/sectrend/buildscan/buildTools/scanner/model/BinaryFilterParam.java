package com.sectrend.buildscan.buildTools.scanner.model;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/1/2 9:32
 **/
@Data
public class BinaryFilterParam {

    private Integer mixedBinaryScanFlag = 0;

    private List<String> mixedBinaryScanFilePathList = new ArrayList<>();

    // 需要进行二进制扫描的文件路径
    List<String> binaryScanList = new ArrayList<>();

    List<String> binaryRealScanList = new ArrayList<>();

    // so文件路径集合
    List<String> soBinaryScanList = new ArrayList<>();

    List<String> soBinaryRealScanList = new ArrayList<>();

    // 用于判断目录下是否全部都是so， jar文件
    Map<String, DirectoryFileStateInfo> directoryPathFileMap = new HashMap<>();


    public void addAll(BinaryFilterParam binaryFilterParam) {
        if (binaryFilterParam == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(binaryFilterParam.getBinaryScanList())) {
            this.binaryScanList.addAll(binaryFilterParam.getBinaryScanList());
        }
        if (CollectionUtils.isNotEmpty(binaryFilterParam.getBinaryRealScanList())) {
            this.binaryRealScanList.addAll(binaryFilterParam.getBinaryRealScanList());
        }
        if (CollectionUtils.isNotEmpty(binaryFilterParam.getSoBinaryScanList())) {
            this.soBinaryScanList.addAll(binaryFilterParam.getSoBinaryScanList());
        }
        if (CollectionUtils.isNotEmpty(binaryFilterParam.getSoBinaryRealScanList())) {
            this.soBinaryRealScanList.addAll(binaryFilterParam.getSoBinaryRealScanList());
        }
        if (binaryFilterParam.getDirectoryPathFileMap() != null && !binaryFilterParam.getDirectoryPathFileMap().isEmpty()) {
            for (Map.Entry<String, DirectoryFileStateInfo> entry : binaryFilterParam.getDirectoryPathFileMap().entrySet()) {
                DirectoryFileStateInfo directoryFileState = this.directoryPathFileMap.get(entry.getKey());
                if (directoryFileState == null) {
                    this.directoryPathFileMap.put(entry.getKey(), entry.getValue());
                    continue;
                }
                if (directoryFileState.getIsAllSoFile() == null || directoryFileState.getIsAllSoFile() == 1) {
                    directoryFileState.setIsAllSoFile(entry.getValue().getIsAllSoFile());
                }
                if (directoryFileState.getIsAllJarFile() == null || directoryFileState.getIsAllJarFile() == 1) {
                    directoryFileState.setIsAllJarFile(entry.getValue().getIsAllJarFile());
                }
            }
        }
    }

}
