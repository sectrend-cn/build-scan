package com.sectrend.buildscan.buildTools.git.build;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.executable.finder.GitFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class GitCliScannable extends Scannable {

    private final FileFinder fileFinder;

    private final GitCliScanExecutor gitCliScanExecutor;

    private final GitFinder gitFinder;

    private File gitExecutable;

    public GitCliScannable(ScannableEnvironment environment, FileFinder fileFinder, GitCliScanExecutor gitCliScanExecutor, GitFinder gitFinder) {
        super(environment);
        this.fileFinder = fileFinder;
        this.gitCliScanExecutor = gitCliScanExecutor;
        this.gitFinder = gitFinder;
    }

    public ScannableResult exeFind() throws ScannableException {
        this.gitExecutable = this.gitFinder.findGit();
        if (this.gitExecutable == null)
            return (ScannableResult)new FailedScannableResult();
        return (ScannableResult)new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        File gitDirectory = this.fileFinder.findFile(this.environment.getDirectory(), ".git");
        if (gitDirectory == null)
            return (ScannableResult) new FailedScannableResult();
        return (ScannableResult)new PassedScannableResult();
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.gitCliScanExecutor.scanExecute(this.gitExecutable, this.environment.getDirectory());
    }
}
