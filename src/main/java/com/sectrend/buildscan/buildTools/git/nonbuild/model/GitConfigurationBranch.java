package com.sectrend.buildscan.buildTools.git.nonbuild.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class GitConfigurationBranch {
    @NotNull
    private final String remoteName;

    @NotNull
    private final String branchMerge;

    @NotNull
    private final String branchName;

    public GitConfigurationBranch(@NotNull String branchName, @NotNull String remoteName, @NotNull String branchMerge) {
        this.branchName = branchName;
        this.remoteName = remoteName;
        this.branchMerge = branchMerge;
    }
}
