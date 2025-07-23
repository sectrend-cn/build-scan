package com.sectrend.buildscan.buildTools.maven.model;

import com.sectrend.buildscan.base.graph.DependencyLocation;
import lombok.Data;

@Data
public class MavenResult {
    private String projectName;

    private String projectVersion;
    private String vendor;

    private DependencyLocation dependencyLocation;

    public MavenResult(String projectName, String projectVersion, DependencyLocation dependencyLocation) {
        this.projectName = projectName;
        this.projectVersion = projectVersion;
//        this.vendor = vendor;
        this.dependencyLocation = dependencyLocation;
    }
}