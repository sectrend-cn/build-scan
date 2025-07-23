package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.go.gomod.nonbuild.GoModGraphAnalyzer;
import com.sectrend.buildscan.configuration.RunBeanConfiguration;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.factory.ScannableFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class GoModExtractHandler implements ExtractHandler {

    private final Logger logger = LoggerFactory.getLogger(GoModExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver) throws Throwable {
        ScannableFactory scannableFactory = RunBeanConfiguration.scannableFactory;

        GoModGraphAnalyzer goModGraphAnalyzer = new GoModGraphAnalyzer(new ForeignIdFactory());
        SimpleFileFinder simpleFileFinder = new SimpleFileFinder();
        Scannable goModScannable = scannableFactory.createGoModScannable(scannableEnvironment, simpleFileFinder, simpleExecutableResolver);

        ScanResults scanResults = null;
        try {
            //gomod走【构建】的场合才会去找exe文件
            if (isExecuteBuild(scannableEnvironment.getBuildScanType()) && goModScannable.fileFind().getPassed() && goModScannable.exeFind().getPassed()) {
                logger.info("execute go mod build dependency scan");
                scanResults = goModScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                scanResults.setBuildFlag(true);
                return scanResults;
            }
        } catch (Exception e) {
            logger.error("go mod build dependency scan error", e);
        }

        if ((Objects.isNull(scanResults) || scanResults.getScanExecuteStatus().equals(ScanResults.ScanExecuteStatus.FAILURE) || scanResults.getScanExecuteStatus().equals(ScanResults.ScanExecuteStatus.EXCEPTION))
                && isExecuteNonBuild(scannableEnvironment.getBuildScanType())) {

            try {
                //gomod走【非构建】的场合, 直接读取
                goModScannable = scannableFactory.createGoModAnalyzeScannable(scannableEnvironment, simpleFileFinder, goModGraphAnalyzer);
                if (goModScannable.fileFind().getPassed()) {
                    logger.info("execute go mod file dependency scan");
                    scanResults = goModScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                    scanResults.setBuildFlag(false);
                }
            } catch (Exception e) {
                logger.error("execute go mod build dependency scan error", e);
            }
        }
        return scanResults;
    }


}
