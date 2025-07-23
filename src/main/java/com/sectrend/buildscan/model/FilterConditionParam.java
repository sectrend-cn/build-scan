package com.sectrend.buildscan.model;

import lombok.Data;

@Data
public class FilterConditionParam {

    private String path;

    private Integer depth;

    private String build_type;
}
