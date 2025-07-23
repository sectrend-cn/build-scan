package com.sectrend.buildscan.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author huishun.yi
 * @Date 2024/2/26 14:44
 */
@Data
public class DependencyVariable {

    private String parentVariableName;

    private String variableName;

    private Map<String, DependencyVariable> childrenVariableMap = new HashMap<>();


    public static DependencyVariable buildGradleVariable() {
        return new DependencyVariable();
    }

}
