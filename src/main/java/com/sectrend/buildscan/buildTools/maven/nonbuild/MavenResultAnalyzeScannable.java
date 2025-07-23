package com.sectrend.buildscan.buildTools.maven.nonbuild;

import com.sectrend.buildscan.buildTools.*;
import com.sectrend.buildscan.result.ScannableResult;
import com.sectrend.buildscan.result.impl.PassedScannableResult;

import java.io.File;

/**
 * 解析Maven本地依赖信息文件
 */
public class MavenResultAnalyzeScannable extends Scannable {

    private final MavenResultAnalyzeScanExecutor mavenResultAnalyzeScanExecutor;

    public MavenResultAnalyzeScannable(ScannableEnvironment environment, MavenResultAnalyzeScanExecutor mavenResultAnalyzeScanExecutor) {
        super(environment);
        this.mavenResultAnalyzeScanExecutor = mavenResultAnalyzeScanExecutor;
    }

    public ScannableResult exeFind() throws ScannableException {
        return (ScannableResult) new PassedScannableResult();
    }

    public ScannableResult fileFind() {
        return (ScannableResult) new PassedScannableResult();
    }

    public ScanResults scanExecute(ScanEnvironment scanEnvironment) {
        return this.mavenResultAnalyzeScanExecutor.scanExecute(new File(this.environment.getBuildTreeFile()));
    }

}
