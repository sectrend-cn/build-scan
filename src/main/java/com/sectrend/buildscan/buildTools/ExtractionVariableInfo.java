package com.sectrend.buildscan.buildTools;

import com.sectrend.buildscan.model.DependencyVariableName;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * @Author huishun.yi
 * @Date 2024/2/27 16:43
 */
@Data
public class ExtractionVariableInfo {

    private List<String> notFoundFiles;

    private Set<DependencyVariableName> dependenciesVariableList;

}
