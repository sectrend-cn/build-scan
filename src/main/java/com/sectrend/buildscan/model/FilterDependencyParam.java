package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.List;

@Data
public class  FilterDependencyParam {

    private String build_type; //mvn

    /**
     * 构建需要排除的依赖类型
     */
    private List<String> exclude;

    /**
     * 构建包含的依赖类型
     */
    private List<String> include; //runtime
}
