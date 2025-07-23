
package com.sectrend.buildscan.buildTools.git.nonbuild.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;


@Getter
@AllArgsConstructor
public class GitConfiguration {

    private final List<GitConfigurationBranch> gitConfigurationBranches;

    private final List<GitConfigurationRemote> gitConfigurationRemotes;
}
