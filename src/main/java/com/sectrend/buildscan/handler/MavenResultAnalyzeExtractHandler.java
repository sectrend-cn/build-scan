package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.Scannable;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.maven.build.MavenDependencyLocationPackager;
import com.sectrend.buildscan.buildTools.maven.nonbuild.MavenResultAnalyzeScanExecutor;
import com.sectrend.buildscan.buildTools.maven.nonbuild.MavenResultAnalyzeScannable;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.utils.CheckUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenResultAnalyzeExtractHandler implements ExtractHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver)  throws Throwable {

        if(!CheckUtils.checkTxtDir(scannableEnvironment.getBuildTreeFile()))
            return null;

        Scannable scannable = new MavenResultAnalyzeScannable(
                scannableEnvironment,
                new MavenResultAnalyzeScanExecutor(new MavenDependencyLocationPackager(new ForeignIdFactory()))
        );

        ScanResults extract = null;
        try {
            logger.info("execute Maven dependencyTree scan");
            extract = scannable.scanExecute(null);
            extract.setBuildFlag(true);

            if (CollectionUtils.isEmpty(extract.getDependencyLocations())) {
                logger.error("Scan failed and unable to parse Maven dependency tree. Please check the content of the dependency file");
            }

        } catch (Exception e) {
            logger.error("Scan failed and unable to parse Maven dependency tree. Please check the content of the dependency file", e);
        }
        return extract;
    }


}
