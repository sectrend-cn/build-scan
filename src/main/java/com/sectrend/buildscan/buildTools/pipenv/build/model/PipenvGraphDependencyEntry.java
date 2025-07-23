package com.sectrend.buildscan.buildTools.pipenv.build.model;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class PipenvGraphDependencyEntry {

    @SerializedName("dependencies")
    private final List<PipenvGraphDependencyNode> dependencies;

    @SerializedName("package_name")
    private final String packageName;

    @SerializedName("installed_version")
    private final String installedVersion;

    public PipenvGraphDependencyEntry(String packageName, String installedVersion, List<PipenvGraphDependencyNode> dependencies) {
        this.packageName = packageName;
        this.installedVersion = installedVersion;
        this.dependencies = dependencies;
    }
}
