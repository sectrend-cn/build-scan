package com.sectrend.buildscan.buildTools.pipenv.nonbuild.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class PipfileLockDependencyEnv {

    @SerializedName("develop")
    public Map<String, PipfileLockDependencyVersion> developDependencies;

    @SerializedName("default")
    public Map<String, PipfileLockDependencyVersion> defaultDependencies;

}
