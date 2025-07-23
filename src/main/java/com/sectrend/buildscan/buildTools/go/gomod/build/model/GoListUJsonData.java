package com.sectrend.buildscan.buildTools.go.gomod.build.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class GoListUJsonData {

    @SerializedName("Replace")
    private SubstituteData replace;

    @SerializedName("Version")
    private String version;

    @SerializedName("Path")
    private String path;
}
