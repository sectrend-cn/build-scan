package com.sectrend.buildscan.model;

import lombok.Data;

/**
 * @Author huishun.yi
 * @Date 2024/2/26 20:22
 */
@Data
public class DependencyVariableName {

    private int line;
    private String variableName;

    private String completeVariableName;

    // 变量使用类型  version(版本), dependency（整个依赖都是变量）
    private String variableUseType;

    private String scope;

}
