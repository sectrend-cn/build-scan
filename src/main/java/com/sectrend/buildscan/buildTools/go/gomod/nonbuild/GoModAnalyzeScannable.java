package com.sectrend.buildscan.buildTools.go.gomod.nonbuild;

import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

/**
 * @Author huishun.yi
 * @Date 2023/3/17 13:57
 */
public class GoModAnalyzeScannable extends Scannable {

    public static final String GOMOD_FILENAME_PATTERN = "go.mod";

    private final FileFinder fileFinder;

    private final GoModAnalyzeScanExecutor scanExecutor;

    public GoModAnalyzeScannable(ScannableEnvironment environment, FileFinder fileFinder, GoModAnalyzeScanExecutor scanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.scanExecutor = scanExecutor;
    }

    @Override
    public ScannableResult exeFind() throws ScannableException {
        return null;
    }

    @Override
    public ScannableResult fileFind() {
        File goModFile = this.fileFinder.findFile(this.environment.getDirectory(), GOMOD_FILENAME_PATTERN);
        if (goModFile == null)
            return (ScannableResult) new FailedScannableResult();
        return new PassedScannableResult();
    }

    @Override
    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return scanExecutor.scanExecute(this.environment.getScannableEnvironmentList());
    }

}
