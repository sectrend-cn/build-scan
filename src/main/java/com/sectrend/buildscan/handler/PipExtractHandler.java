package com.sectrend.buildscan.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.ScanEnvironment;
import com.sectrend.buildscan.buildTools.pip.PipBuildScannable;
import com.sectrend.buildscan.buildTools.pip.PipBuildScannableParams;
import com.sectrend.buildscan.buildTools.pip.PipBuildScanExecutor;
import com.sectrend.buildscan.buildTools.pip.PipBuildTreeAnalyzer;
import com.sectrend.buildscan.enums.DetectBusinessParams;
import com.sectrend.buildscan.executable.impl.LocalPipBuilderFinder;
import com.sectrend.buildscan.executable.impl.SimpleExecutableResolver;
import com.sectrend.buildscan.executable.impl.SimpleExecutableRunner;
import com.sectrend.buildscan.executable.finder.PipFinder;
import com.sectrend.buildscan.executable.finder.PythonFinder;
import com.sectrend.buildscan.factory.ForeignIdFactory;
import com.sectrend.buildscan.finder.impl.SimpleFileFinder;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class PipExtractHandler implements ExtractHandler{

    private final static Logger logger = LoggerFactory.getLogger(PipExtractHandler.class);

    @Override
    public ScanResults handler(ScannableEnvironment scannableEnvironment, String taskDir, SimpleExecutableResolver simpleExecutableResolver)  throws Throwable {
        // 参数获取及处理
        Properties arguments = scannableEnvironment.getArguments();
        Set<Path> pipRequirementsPathList = new HashSet<>();;
        String pipProjectName = null;
        if (arguments != null) {
            String pipRequirementsPaths = arguments.getProperty(DetectBusinessParams.PIP_REQUIREMENTS_PATH.getAttributeName());

            logger.info("arg -> pipRequirementsPath is : {}", pipRequirementsPaths);
            if (StringUtils.isNotBlank(pipRequirementsPaths)) {
                try {
                    Gson gson = new GsonBuilder().create();

                    // 判断 pipRequirementsPaths 是否为单个路径还是数组
                    if (pipRequirementsPaths.startsWith("[") && pipRequirementsPaths.endsWith("]")) {
                        // 输入是一个数组
                        Type listType = new TypeToken<List<String>>() {}.getType();
                        List<String> pathList = gson.fromJson(pipRequirementsPaths, listType);
                        for (String path : pathList) {
                            pipRequirementsPathList.add(Paths.get(path));
                        }
                    } else {
                        // 输入是一个单个路径
                        pipRequirementsPathList.add(Paths.get(pipRequirementsPaths));
                    }
                } catch (Exception e) {
                    logger.error("pipRequirementsPath structural abnormality", e);
                }
            }
            pipProjectName = arguments.getProperty(DetectBusinessParams.PIP_PROJECT_NAME.getAttributeName());
        } else {
            scannableEnvironment.setArguments(new Properties());
        }

        PipBuildScannable pipBuildScannable = new PipBuildScannable(
                scannableEnvironment,
                new SimpleFileFinder(),
                (PythonFinder)simpleExecutableResolver,
                (PipFinder)simpleExecutableResolver,
                new LocalPipBuilderFinder(DirectoryManager.getDirectoryManager()),
                new PipBuildScanExecutor(new SimpleExecutableRunner(), new PipBuildTreeAnalyzer(new ForeignIdFactory())),
                new PipBuildScannableParams(pipProjectName,pipRequirementsPathList)
        );

        ScanResults extract = null;
        try {
            if (isExecuteBuild(scannableEnvironment.getBuildScanType()) && pipBuildScannable.fileFind().getPassed() && pipBuildScannable.exeFind().getPassed()){
                logger.info("execute pip build dependency scan");
                extract = pipBuildScannable.scanExecute(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(true);
            }
        } catch (ScannableException e) {
            logger.error("execute pip build dependency scan error", e);
        }

        if(extract != null && CollectionUtils.isNotEmpty(extract.getDependencyLocations()) &&
                extract.getDependencyLocations().get(0).getDependencyGraph() != null
            && CollectionUtils.isNotEmpty(extract.getDependencyLocations().get(0).getDependencyGraph().getRootDependencies())
                &&  !extract.getDependencyLocations().get(0).getDependencyGraph().getRelationships().isEmpty()){
            return extract;
        }

        try {
            if (isExecuteNonBuild(scannableEnvironment.getBuildScanType()) && pipBuildScannable.fileFind().getPassed()){
                logger.info("execute pip file dependency scan");
                extract = pipBuildScannable.readTheFile(new ScanEnvironment(new File(taskDir)));
                extract.setBuildFlag(false);
            }
        } catch (Exception e) {
            logger.error("execute pip file dependency scan error", e);
        }

        if(extract == null){
            return (new ScanResults.Builder()).failure("pip scan error").build();
        }
        return extract;
    }
}
