package com.sectrend.buildscan.buildTools.go.gomod.build;


import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.executable.finder.GoFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class GoModCliScannable extends Scannable {

    public static final String GOMOD_FILENAME_PATTERN = "go.mod";

    private final FileFinder fileFinder;

    private final GoFinder goFinder;

    private final GoModCliScanExecutor goModCliScanExecutor;

    private File goExe;

    public GoModCliScannable(ScannableEnvironment environment, FileFinder fileFinder, GoFinder goFinder, GoModCliScanExecutor goModCliScanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.goFinder = goFinder;
        this.goModCliScanExecutor = goModCliScanExecutor;
    }

    @Override
    public ScannableResult exeFind() throws ScannableException {
        // 查找go可执行文件
        this.goExe = this.goFinder.findGo(this.environment);
        if (this.goExe == null)
            return new FailedScannableResult();
        return new PassedScannableResult();
    }

    @Override
    public ScannableResult fileFind() {
        File found = this.fileFinder.findFile(this.environment.getDirectory(), GOMOD_FILENAME_PATTERN);
        if(found == null)
            return (ScannableResult) new FailedScannableResult();
        return new PassedScannableResult();
    }

    @Override
    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.goModCliScanExecutor.scanExecute(this.environment, this.goExe);
    }

}
