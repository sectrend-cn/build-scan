package com.sectrend.buildscan.enums;

/**
 * @Author biao.yang
 * @Date 2024/1/25 20:41
 */
public enum AuthType {
    Cookie("Cookie"),
    Token("Token");

    private String value;

    AuthType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String desc) {
        this.value = desc;
    }
}
