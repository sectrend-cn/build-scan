package com.sectrend.buildscan.enums;

public enum SourceType {

    SOURCE_CLI_TYPE("cli"),
    SOURCE_JENKINS_TYPE("jenkins"),
    SOURCE_UI_TYPE("ui"),
    SOURCE_PLUGIN_IDEA("plugin_idea");

    private String value;

    SourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String desc) {
        this.value = desc;
    }
}
