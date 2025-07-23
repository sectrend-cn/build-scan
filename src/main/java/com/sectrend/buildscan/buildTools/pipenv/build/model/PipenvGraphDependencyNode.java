package com.sectrend.buildscan.buildTools.pipenv.build.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PipenvGraphDependencyNode {

    @SerializedName("package_name")
    private final String packageName;

    @SerializedName("installed_version")
    private final String installedVersion;

    @SerializedName("dependencies")
    private final List<PipenvGraphDependencyNode> dependencies;
}
