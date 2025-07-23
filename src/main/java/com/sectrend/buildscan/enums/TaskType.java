package com.sectrend.buildscan.enums;

/**
 * 任务类型：
 *      FINGER_TASK_TYPE ：单独生成指纹文件
 *      TASK_SCAN_TYPE ：执行扫描任务
 */
public enum TaskType {

    FINGER_TASK_TYPE("finger"),
    TASK_SCAN_TYPE("taskScan");

    private String value;

    TaskType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String desc) {
        this.value = desc;
    }

}
