package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.executable.finder.MavenFinder;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

public class MavenPomScannable extends Scannable {
    public static final String POM_FILENAME = "pom.xml";

    private final FileFinder fileFinder;

    private final MavenFinder mavenFinder;

    private final MavenCliScanExecutor mavenCliScanExecutor;

    private File mavenExe;


    public MavenPomScannable(ScannableEnvironment environment, FileFinder fileFinder, MavenFinder mavenFinder, MavenCliScanExecutor mavenCliScanExecutor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mavenFinder = mavenFinder;
        this.mavenCliScanExecutor = mavenCliScanExecutor;
    }

    public ScannableResult exeFind() throws ScannableException {
        this.mavenExe = this.mavenFinder.findMaven(this.environment);
        if (this.mavenExe == null)
            return (ScannableResult) new FailedScannableResult();
        return (ScannableResult) new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        File pom = this.fileFinder.findFile(this.environment.getDirectory(), POM_FILENAME);
        if (pom == null)
            return (ScannableResult) new FailedScannableResult();
        return (ScannableResult) new PassedScannableResult();
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.mavenCliScanExecutor.scanExecute(this.environment,this.environment.getDirectory(), this.mavenExe);
    }

}
