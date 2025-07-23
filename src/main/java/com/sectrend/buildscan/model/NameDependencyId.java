package com.sectrend.buildscan.model;

public class NameDependencyId extends DependencyId {

    private String name;

    public NameDependencyId(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
