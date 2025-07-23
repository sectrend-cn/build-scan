package com.sectrend.buildscan.buildTools.go.godep.nonbuild.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

@Data
public class GoDepProject {

    @SerializedName("name")
    private String name;
    @SerializedName("version")
    private String version;
    @SerializedName("revision")
    private String revision;
    @SerializedName("branch")
    private String branch;
    @SerializedName("source")
    private String source;
    @SerializedName("packages")
    private List<String> packages;
}
