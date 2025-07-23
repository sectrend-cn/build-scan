package com.sectrend.buildscan.enums;

/**
 *   扫描类型 ：
 *          SOURCE_SCAN_TYPE： 扫描源码上传
 *          DOCKER_SCAN_TYPE： docker文件上传
 *          BINARY_SCAN_TYPE： binary文件上传
 */
public enum ScanType {

    SOURCE_SCAN_TYPE("source"),
    DOCKER_SCAN_TYPE("docker"),
    BINARY_SCAN_TYPE("binary");


    private String value;

    ScanType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String desc) {
        this.value = desc;
    }

}
