package com.sectrend.buildscan.buildTools.pipenv.nonbuild;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class PipfileLockScannable extends Scannable {
    private static final String PIPFILE_FILENAME = "Pipfile";
    private static final String PIPFILE_LOCK_FILENAME = "Pipfile.lock";

    private final PipfileLockScannableParams pipfileLockScannableParams;

    private final FileFinder fileFinder;
    private final PipfileLockScanExecutor pipfileLockScanExecutor;

    private File pipfileLock;
    private File pipfile;

    public PipfileLockScannable(
            ScannableEnvironment environment,
            FileFinder fileFinder,
            PipfileLockScanExecutor pipfileLockScanExecutor,
            PipfileLockScannableParams pipfileLockScannableParams
    ) {
        super(environment);
        this.fileFinder = fileFinder;
        this.pipfileLockScanExecutor = pipfileLockScanExecutor;
        this.pipfileLockScannableParams = pipfileLockScannableParams;
    }

    @Override
    public ScannableResult exeFind() throws ScannableException {
        return new PassedScannableResult();
    }

    @Override
    public ScannableResult fileFind() {
        this.pipfileLock = this.fileFinder.findFile(this.environment.getDirectory(), PIPFILE_LOCK_FILENAME);
        this.pipfile = this.fileFinder.findFile(this.environment.getDirectory(), PIPFILE_FILENAME);
        if (pipfileLock == null && pipfile == null) {
            return (ScannableResult) new FailedScannableResult();
        } else {
            return new PassedScannableResult();
        }
    }

    @Override
    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return pipfileLockScanExecutor.scanExecute(pipfileLock, this.pipfileLockScannableParams.getDependencyTypeFilter(),this.environment.getDirectory(),this.pipfileLockScannableParams.getPipProjectName().orElse(""));
    }
}