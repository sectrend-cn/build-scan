package com.sectrend.buildscan.model;

public class StringDependencyId extends DependencyId{

    private String value;

    public StringDependencyId(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
