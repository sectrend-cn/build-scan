package com.sectrend.buildscan.handler;

import com.google.gson.Gson;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipenvDependencyType;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockScannable;
import com.sectrend.buildscan.buildTools.pipenv.nonbuild.PipfileLockScannableParams;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvConverter;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvScanExecutor;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvScannable;
import com.sectrend.buildscan.buildTools.pipenv.build.PipenvScannableParams;
import com.sectrend.buildscan.configuration.RunBeanConfiguration;
import com.sectrend.buildscan.enums.DetectBusinessParams;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.executable.impl.SimpleExecutableRunner;
import com.sectrend.buildscan.factory.ScannableFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import com.sectrend.buildscan.utils.EnumUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class PipenvExtractHandler implements ExtractHandler {
    private final Logger logger = LoggerFactory.getLogger(PipenvExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver)  throws Throwable{
        ScannableFactory scannableFactory = RunBeanConfiguration.scannableFactory;

        // 参数提取
        Properties arguments = scannableEnvironment.getArguments();
        boolean pipProjectTreeOnly = false;
        Set<PipenvDependencyType> pipfileDependencyTypesExcludedSet = EnumSet.noneOf(PipenvDependencyType.class);
        String pipenvProjectName = null;
        String pipenvProjectVersionName = null;
        if (arguments != null) {
            pipProjectTreeOnly = "1".equals(arguments.getProperty(DetectBusinessParams.PIPENV_ONLY_PROJECT_TREE.getAttributeName()));

            String pipfileDependencyTypesExcluded = arguments.getProperty(DetectBusinessParams.PIPENV_DEPENDENCY_TYPES_EXCLUDED.getAttributeName());
            pipfileDependencyTypesExcluded = StringUtils.isBlank(pipfileDependencyTypesExcluded) ? "NONE" : pipfileDependencyTypesExcluded;
            pipfileDependencyTypesExcludedSet = Arrays.stream(pipfileDependencyTypesExcluded.split(","))
                    .map(PipenvDependencyType::valueOf)
                    .collect(Collectors.toSet());

            pipenvProjectName = arguments.getProperty(DetectBusinessParams.PIPENV_PROJECT_NAME.getAttributeName());
            pipenvProjectVersionName = arguments.getProperty(DetectBusinessParams.PIPENV_PROJECT_VERSION_NAME.getAttributeName());
        } else {
            scannableEnvironment.setArguments(new Properties());
        }

        PipenvScannable pipenvScannable = new PipenvScannable(
                scannableEnvironment,
                new PipenvScannableParams(pipenvProjectName, pipenvProjectVersionName, pipProjectTreeOnly),
                new SimpleFileFinder(),

                simpleExecutableResolver,
                simpleExecutableResolver,
                new PipenvScanExecutor(new SimpleExecutableRunner(), new PipenvConverter(), new Gson()));
        ScanResults extract = null;
        try {
            if (isExecuteBuild(scannableEnvironment.getBuildScanType()) && pipenvScannable.fileFind().getPassed() && pipenvScannable.exeFind().getPassed()) {
                logger.info("execute pipenv build dependency scan ");
                extract = pipenvScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(true);
            }
        } catch (ScannableException e) {
            logger.error("execute pipenv build dependency scan error", e);
        }

        if(extract != null && CollectionUtils.isNotEmpty(extract.getDependencyLocations()) &&
                extract.getDependencyLocations().get(0).getDependencyGraph() != null
                && CollectionUtils.isNotEmpty(extract.getDependencyLocations().get(0).getDependencyGraph().getRootDependencies())
                &&  !extract.getDependencyLocations().get(0).getDependencyGraph().getRelationships().isEmpty()){
            return extract;
        }

        PipfileLockScannable pipfileLockScannable = scannableFactory.createPipfileLockScannable(scannableEnvironment,new PipfileLockScannableParams(EnumUtil.fromExcluded(pipfileDependencyTypesExcludedSet), pipenvProjectName));

        try {
            if (isExecuteNonBuild(scannableEnvironment.getBuildScanType()) && pipfileLockScannable.fileFind().getPassed()) {
                logger.info("execute pipenv file dependency scan");
                extract = pipfileLockScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(false);
            }
        } catch (Exception e) {
            logger.error("execute pipenv file dependency scan error", e);
        }

        return extract;
    }
}
