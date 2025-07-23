package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.git.build.GitCliScannable;
import com.sectrend.buildscan.buildTools.git.build.GitCliScanExecutor;
import com.sectrend.buildscan.buildTools.git.build.GitUrlStringAnalyzer;
import com.sectrend.buildscan.buildTools.git.nonbuild.GitAnalyzeScannable;
import com.sectrend.buildscan.buildTools.git.nonbuild.GitAnalyzeScanExecutor;
import com.sectrend.buildscan.buildTools.git.nonbuild.analyze.GitConfigurationNameVersionConverter;
import com.sectrend.buildscan.configuration.RunBeanConfiguration;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitExtractHandler implements ExtractHandler {

    private final Logger logger = LoggerFactory.getLogger(GitExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver)  throws Throwable {

        GitAnalyzeScannable gitAnalyzeScannable = new GitAnalyzeScannable(scannableEnvironment, new SimpleFileFinder(),
                new GitAnalyzeScanExecutor(new GitConfigurationNameVersionConverter(new GitUrlStringAnalyzer())));

        ScanResults extract = null;

        try {
            if (gitAnalyzeScannable.fileFind().getPassed() && gitAnalyzeScannable.exeFind().getPassed()) {
                logger.debug("File parsing project information");
                extract = gitAnalyzeScannable.scanExecute(new ScanEnvironment(scannableEnvironment.getDirectory()));
            }
        } catch (Exception e) {
            logger.error("File parsing project information error", e);
        }


        if (extract != null) {
            return extract;
        }

        GitCliScannable gitCliScannable = new GitCliScannable(scannableEnvironment, new SimpleFileFinder(),
                new GitCliScanExecutor(RunBeanConfiguration.executableRunner, new GitUrlStringAnalyzer()),
                simpleExecutableResolver);

        try {
            if (gitCliScannable.fileFind().getPassed() && gitCliScannable.exeFind().getPassed()) {
                logger.debug("Remote access to project information");
                extract = gitCliScannable.scanExecute(new ScanEnvironment(scannableEnvironment.getDirectory()));
            }
        } catch (ScannableException e) {
            logger.error("Remote access to project information error", e);
        }
        return extract;
    }
}
