package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.maven.model.MavenAnalyzeParams;
import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.FailedScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;


public class MavenAnalyzeScannable extends Scannable {

    private final FileFinder fileFinder;

    private final MavenAnalyzeScanExecutor mavenAnalyzeScanExecutor;

    private final MavenAnalyzeParams mavenAnalyzeParams;

    private File pomXmlFile;

    public MavenAnalyzeScannable(ScannableEnvironment environment, FileFinder fileFinder, MavenAnalyzeScanExecutor mavenAnalyzeScanExecutor, MavenAnalyzeParams mavenAnalyzeParams) {
        super(environment);
        this.fileFinder = fileFinder;
        this.mavenAnalyzeScanExecutor = mavenAnalyzeScanExecutor;
        this.mavenAnalyzeParams = mavenAnalyzeParams;
    }

    public ScannableResult exeFind() {
        return new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        this.pomXmlFile = this.fileFinder.findFile(this.environment.getDirectory(), "pom.xml");
        if (this.pomXmlFile == null)
            return (ScannableResult) new FailedScannableResult();
        return new PassedScannableResult();
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.mavenAnalyzeScanExecutor.scanExecute(this.environment.getScannableEnvironmentList(), this.mavenAnalyzeParams,this.environment.getArguments());
    }


}
