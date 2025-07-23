package com.sectrend.buildscan.buildTools.git.build;

import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.ExeRunnerException;
import com.sectrend.buildscan.executable.SynthesisException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

public class GitCliScanExecutor {

    private final static Logger logger = LoggerFactory.getLogger(GitCliScanExecutor.class);

    private final ExecutableRunner executableRunner;

    private final GitUrlStringAnalyzer gitUrlStringAnalyzer;

    public GitCliScanExecutor(ExecutableRunner executableRunner, GitUrlStringAnalyzer gitUrlStringAnalyzer) {
        this.executableRunner = executableRunner;
        this.gitUrlStringAnalyzer = gitUrlStringAnalyzer;
    }

    public ScanResults scanExecute(File gitExecutable, File directory) {
        try {
            String repoName = getRepoName(gitExecutable, directory);
            String branch = getRepoBranch(gitExecutable, directory);
            if ("HEAD".equals(branch)) {
                this.logger.info("The HEAD of this repository is detached, using heuristics to find Git branches.");
                branch = getRepoBranchBackup(gitExecutable, directory).orElseGet(() -> getCommitHash(gitExecutable, directory));
            }
            return (new ScanResults.Builder())
                    .success()
                    .scanProjectName(repoName)
                    .scanProjectVersion(branch)
                    .build();
        } catch (ExeRunnerException | SynthesisException | MalformedURLException e) {
            this.logger.debug("Project information cannot be extracted from a git executable.", e);
            return (new ScanResults.Builder())
                    .success()
                    .build();
        }
    }

    private String getRepoName(File gitExecutable, File directory) throws ExeRunnerException, SynthesisException, MalformedURLException {
        String remoteUrlString = runGitSingleLinesResponse(gitExecutable, directory, new String[] { "config", "--get", "remote.origin.url" });
        return this.gitUrlStringAnalyzer.getRepositoryName(remoteUrlString);
    }

    private String getRepoBranch(File gitExecutable, File directory) throws ExeRunnerException, SynthesisException {
        return runGitSingleLinesResponse(gitExecutable, directory, new String[] { "rev-parse", "--abbrev-ref", "HEAD" }).trim();
    }

    private Optional<String> getRepoBranchBackup(File gitExecutable, File directory) throws ExeRunnerException, SynthesisException {
        String repoBranch, output = runGitSingleLinesResponse(gitExecutable, directory, new String[] { "log", "-n", "1", "--pretty=%d", "HEAD" }).trim();
        output = StringUtils.removeStart(output, "(");
        output = StringUtils.removeEnd(output, ")");
        String[] pieces = output.split(", ");
        if (pieces.length != 2 || !pieces[1].startsWith("tag: ")) {
            this.logger.debug(String.format("Unexpected output on git log. %s", new Object[] { output }));
            repoBranch = null;
        } else {
            repoBranch = pieces[1].replace("tag: ", "").trim();
        }
        return Optional.ofNullable(repoBranch);
    }

    private String getCommitHash(File gitExecutable, File directory) {
        try {
            return runGitSingleLinesResponse(gitExecutable, directory, new String[] { "rev-parse", "HEAD" }).trim();
        } catch (ExeRunnerException | SynthesisException e) {
            return "";
        }
    }

    private String runGitSingleLinesResponse(File gitExecutable, File directory, String... commands) throws ExeRunnerException, SynthesisException {
        ExecutionOutput gitOutput = this.executableRunner.execute(directory, gitExecutable, commands);
        if (gitOutput.getExitCode() != 0)
            throw new SynthesisException("git returned a non-zero status code.");
        List<String> lines = gitOutput.getStandardOutputAsList();
        if (lines.size() != 1)
            throw new SynthesisException("git output has different expected sizes.");
        return ((String)lines.get(0)).trim();
    }
}
