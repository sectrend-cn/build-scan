package com.sectrend.buildscan.buildTools.git.nonbuild;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class GitAnalyzeScannable extends Scannable {

    private final FileFinder fileFinder;

    private final GitAnalyzeScanExecutor gitAnalyzeScanExecutor;

    private File gitConfigFile;

    private File gitHeadFile;

    public GitAnalyzeScannable(ScannableEnvironment environment, FileFinder fileFinder, GitAnalyzeScanExecutor gitAnalyzeScanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.gitAnalyzeScanExecutor = gitAnalyzeScanExecutor;
    }

    public ScannableResult exeFind() {
        return (ScannableResult)new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        File gitDirectory = this.fileFinder.findFile(this.environment.getDirectory(), ".git");
        if (gitDirectory == null)
            return (ScannableResult) new FailedScannableResult();
        this.gitConfigFile = this.fileFinder.findFile(gitDirectory, "config");
        this.gitHeadFile = this.fileFinder.findFile(gitDirectory, "HEAD");
        if (this.gitConfigFile == null)
            return (ScannableResult) new FailedScannableResult();
        if (this.gitHeadFile == null)
            return (ScannableResult) new FailedScannableResult();
        return (ScannableResult)new PassedScannableResult();
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.gitAnalyzeScanExecutor.scanExecute(this.gitConfigFile, this.gitHeadFile);
    }
}
