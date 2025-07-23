package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.go.govendor.nonbuild.GoVendorScannable;
import com.sectrend.buildscan.buildTools.go.govendor.nonbuild.GoVendorScanExecutor;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class GoVendorExtractHandler implements ExtractHandler {

    private final Logger logger = LoggerFactory.getLogger(GoVendorExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver) throws Throwable {
        Scannable goVendorScannable = new GoVendorScannable(
                scannableEnvironment,
                new SimpleFileFinder(),
                new GoVendorScanExecutor(new ForeignIdFactory()));

        ScanResults scanResults = null;
        try {
            logger.info("execute go vendor scan, build flag = false");
            if (isExecuteNonBuild(scannableEnvironment.getBuildScanType()) && goVendorScannable.fileFind().getPassed() && goVendorScannable.exeFind().getPassed()) {
                scanResults = goVendorScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                //goVendor目前只有非构建
                scanResults.setBuildFlag(false);
            }
        } catch (Exception e) {
            logger.error("go vendor failed:{}, build flag = false", e.getMessage());
        }
        return scanResults;
    }

}

