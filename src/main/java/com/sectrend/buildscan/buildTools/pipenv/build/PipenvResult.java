package com.sectrend.buildscan.buildTools.pipenv.build;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PipenvResult {

    private final String projectName;

    private final String projectVersion;

    private final DependencyLocation dependencyLocation;

}
