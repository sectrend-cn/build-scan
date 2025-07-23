package com.sectrend.buildscan.handler;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.maven.build.*;
import com.sectrend.buildscan.buildTools.maven.model.MavenAnalyzeParams;
import com.sectrend.buildscan.configuration.RunBeanConfiguration;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.factory.ScannableFactory;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;

public class MavenExtractHandler implements ExtractHandler {
    private final Logger logger = LoggerFactory.getLogger(MavenExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver)  throws Throwable {
        ScannableFactory scannableFactory = RunBeanConfiguration.scannableFactory;

        MavenPomScannable mavenPomScannable = scannableFactory.createMavenPomScannable(scannableEnvironment, simpleExecutableResolver);
        ScanResults extract = null;
        try {
            // 判断是否有pom.xml
            if (isExecuteBuild(scannableEnvironment.getBuildScanType()) && mavenPomScannable.fileFind().getPassed() && mavenPomScannable.exeFind().getPassed()) {
                this.logger.info("execute mvn build dependency scan");
                extract = mavenPomScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(true);
            }
        } catch (Exception e) {
            //e.printStackTrace();
            logger.error("mvn build dependency scan error ", e);
        }


        if (extract == null || extract.getScanExecuteStatus().equals(ScanResults.ScanExecuteStatus.FAILURE) || extract.getScanExecuteStatus().equals(ScanResults.ScanExecuteStatus.EXCEPTION)) {
            this.logger.info("Unable to extract project information from git configuration");
            MavenAnalyzeScannable mavenAnalyzeScannable = new MavenAnalyzeScannable(
                    scannableEnvironment, new SimpleFileFinder(), new MavenAnalyzeScanExecutor(new ForeignIdFactory(), saxParser()), new MavenAnalyzeParams(false));
            if(isExecuteNonBuild(scannableEnvironment.getBuildScanType()) && mavenAnalyzeScannable.fileFind().getPassed()){
                extract = mavenAnalyzeScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(false);
            }
        }
        return extract;
    }

    private SAXParser saxParser() {
        try {
            return SAXParserFactory.newInstance().newSAXParser();
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new RuntimeException("Unable to create parser", e);
        }
    }
}
