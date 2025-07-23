package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.go.godep.nonbuild.GoDepScanExecutor;
import com.sectrend.buildscan.buildTools.go.godep.nonbuild.GoDepLockScannable;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoDepExtractHandler implements ExtractHandler {

    private final Logger logger = LoggerFactory.getLogger(GoDepExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver) throws Throwable {

        GoDepScanExecutor goDepScanExecutor = new GoDepScanExecutor(
                new ForeignIdFactory()
        );

        Scannable scannable = new GoDepLockScannable(
                scannableEnvironment,
                new SimpleFileFinder(),
                goDepScanExecutor);

        DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
        ScanResults extract = null;
        try {
            if (isExecuteNonBuild(scannableEnvironment.getBuildScanType()) && scannable.fileFind().getPassed()) {
                logger.info("execute go dep file dependency scan");
                extract = scannable.scanExecute(new ScanEnvironment(directoryManager.getRunsOutputDirectory()));
                extract.setBuildFlag(false);
            }
        } catch (Exception e) {
            logger.error("execute go dep file dependency scan", e);
        }
        return extract;
    }
}
