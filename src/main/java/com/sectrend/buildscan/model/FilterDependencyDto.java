package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.List;

@Data
public class  FilterDependencyDto {


    private String buildType; //mvn

    /**
     * 构建需要排除的依赖类型
     */
    private List<String> exclude;

    /**
     * 构建包含的依赖类型
     */
    private List<String> include; //runtime


}
