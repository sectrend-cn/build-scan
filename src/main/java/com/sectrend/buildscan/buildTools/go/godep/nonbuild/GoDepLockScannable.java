package com.sectrend.buildscan.buildTools.go.godep.nonbuild;

import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class GoDepLockScannable extends Scannable {
    public static final String GOPKG_LOCK_FILENAME = "Gopkg.lock";

    private final FileFinder fileFinder;

    private final GoDepScanExecutor goDepScanExecutor;

    private File goLock;

    public GoDepLockScannable(ScannableEnvironment environment, FileFinder fileFinder, GoDepScanExecutor goDepScanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.goDepScanExecutor = goDepScanExecutor;
    }

    public ScannableResult exeFind() {
        return new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        this.goLock = this.fileFinder.findFile(this.environment.getDirectory(), GOPKG_LOCK_FILENAME);
        if (this.goLock == null) {
            return (ScannableResult) new FailedScannableResult();
        }
        return new PassedScannableResult();
    }


    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        try (InputStream inputStream = Files.newInputStream(this.goLock.toPath())) {
            return this.goDepScanExecutor.scanExecute(inputStream, goLock);
        } catch (IOException e) {
            return (new ScanResults.Builder()).exception(e).build();
        }
    }
}
