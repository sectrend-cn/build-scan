package com.sectrend.buildscan.buildTools.go.godep.nonbuild.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GoLockEntry {

    @SerializedName("projects")
    public List<GoDepProject> goDepProjects;

}
