package com.sectrend.buildscan.buildTools.git.nonbuild.model;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class GitConfigurationRemote {

    @NotNull
    private final String gitUrl;

    @NotNull
    private final String gitFetch;

    @NotNull
    private final String remoteName;

    public GitConfigurationRemote(@NotNull String remoteName, @NotNull String gitUrl, @NotNull String gitFetch) {
        this.remoteName = remoteName;
        this.gitUrl = gitUrl;
        this.gitFetch = gitFetch;
    }
}
