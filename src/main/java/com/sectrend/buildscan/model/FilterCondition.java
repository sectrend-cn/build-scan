package com.sectrend.buildscan.model;


import lombok.Data;

@Data
public class FilterCondition {

    private String path;

    private Integer depth;

    private String buildType;
}
