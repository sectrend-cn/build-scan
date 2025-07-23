package com.sectrend.buildscan.buildTools.git.nonbuild.analyze;

import com.sectrend.buildscan.buildTools.git.build.GitUrlStringAnalyzer;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfiguration;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfigurationBranch;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfigurationRemote;
import com.sectrend.buildscan.executable.SynthesisException;
import com.sectrend.buildscan.utils.NameVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Optional;

public class GitConfigurationNameVersionConverter {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final GitUrlStringAnalyzer gitUrlStringAnalyzer;

    public GitConfigurationNameVersionConverter(GitUrlStringAnalyzer gitUrlStringAnalyzer) {
        this.gitUrlStringAnalyzer = gitUrlStringAnalyzer;
    }

    public NameVersion conversionToProjectInfo(GitConfiguration gitConfiguration, String gitHead) throws SynthesisException, MalformedURLException {
        String projectName, projectVersionName;
        Optional<GitConfigurationBranch> currentBranch = gitConfiguration.getGitConfigurationBranches().stream().filter(it -> it.getBranchMerge().equalsIgnoreCase(gitHead)).findFirst();
        if (currentBranch.isPresent()) {
            this.logger.debug(String.format("Parsing a git repository on branch '%s'.", new Object[] { ((GitConfigurationBranch)currentBranch.get()).getBranchName() }));
            String remoteName = ((GitConfigurationBranch)currentBranch.get()).getRemoteName();
            String remoteUrl = (String) gitConfiguration.getGitConfigurationRemotes().stream().filter(it -> it.getRemoteName().equals(remoteName)).map(GitConfigurationRemote::getGitUrl).findFirst().orElseThrow(() -> new SynthesisException(String.format("Failed to find a url for remote '%s'.", new Object[] { remoteName })));
            projectName = this.gitUrlStringAnalyzer.getRepositoryName(remoteUrl);
            projectVersionName = ((GitConfigurationBranch)currentBranch.get()).getBranchName();
        } else {
            this.logger.debug(String.format("Parsing a git repository with detached head '%s'.", new Object[] { gitHead }));
            String remoteUrl = (String) gitConfiguration.getGitConfigurationRemotes().stream().findFirst().map(GitConfigurationRemote::getGitUrl).orElseThrow(() -> new SynthesisException("No remote urls were found in config."));
            projectName = this.gitUrlStringAnalyzer.getRepositoryName(remoteUrl);
            if(gitHead.contains("/")){
                String[] split = gitHead.split("/");
                projectVersionName = split[split.length -1];
            }else {
                projectVersionName = gitHead;
            }
        }
        return new NameVersion(projectName, projectVersionName);
    }
}
