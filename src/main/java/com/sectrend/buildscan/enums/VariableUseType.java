package com.sectrend.buildscan.enums;

/**
 * @Author huishun.yi
 * @Date 2024/2/27 15:13
 */
public enum VariableUseType {

    VERSION("version"),
    DEPENDENCY("dependency");

    private String value;

    VariableUseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String desc) {
        this.value = desc;
    }

}
