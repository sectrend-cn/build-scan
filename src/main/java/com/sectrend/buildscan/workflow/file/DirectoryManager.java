
package com.sectrend.buildscan.workflow.file;

import com.sectrend.buildscan.workflow.ScanRun;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DirectoryManager {

    private static Logger logger = LoggerFactory.getLogger(DirectoryManager.class);

    private final File userHome;
    private final File runDirectory;
    private File getToolPath;

    private final Map<DirectoryManager.OutputDirectory, File> outputDirectories = new HashMap();
    private final Map<DirectoryManager.RunDirectory, File> runDirectories = new HashMap();

    private volatile static DirectoryManager directoryManager;

    public DirectoryManager(DirectoryParams directoryParams, ScanRun scanRun) {
        this.userHome = new File(System.getProperty("user.home"));
        File outputDirectory = directoryParams.getOutputPathOverride().map(Path::toFile).orElse(new File(this.userHome, "cleansource"));

        this.getToolPath = new File(outputDirectory.getAbsolutePath() + File.separatorChar + DirectoryManager.OutputDirectory.TOOLS.getDirectoryName());

        if (outputDirectory.getAbsolutePath().contains("systemprofile")) {
            this.logger.warn("You appear to be running in 'systemprofile' which can happen when detect is invoked by a system account or as a service.");
            this.logger.warn("If detect has full access to the output directory, no further action is necessary.");
            this.logger.warn("However, this folder typically has restricted access and may cause exceptions in scan.");
            //this.logger.warn("To ensure continued operation, supply an output directory using " + DetectProperties.Companion.getDETECT_OUTPUT_PATH().getName() + " in the future.");
        }

        EnumSet.allOf(DirectoryManager.OutputDirectory.class).stream().filter(it -> !this.outputDirectories.containsKey(it)).forEach((it) -> {
            this.outputDirectories.put(it, new File(outputDirectory, it.getDirectoryName()));
        });
        File possibleRunDirectory = new File(this.getOutputDirectory(DirectoryManager.OutputDirectory.RUNS), scanRun.getRunId());
        if (possibleRunDirectory.exists()) {
            this.logger.warn("A run directory already exists with this detect run id. Will attempt to use a UUID for the run folder in addition.");
            possibleRunDirectory = new File(this.getOutputDirectory(DirectoryManager.OutputDirectory.RUNS), scanRun.getRunId() + "-" + UUID.randomUUID());
        }

        this.runDirectory = possibleRunDirectory;
        EnumSet.allOf(DirectoryManager.RunDirectory.class).forEach((it) -> {
            this.runDirectories.put(it, new File(this.runDirectory, it.getDirectoryName()));
        });

    }


    public static DirectoryManager getDirectoryManager() {
        return getDirectoryManager(null);
    }


    /**
     * 获取 DirectoryManager 单例对象
     * @param outputPathStr
     * @return
     */
    public static DirectoryManager getDirectoryManager(String outputPathStr) {

        Path outputPath = null;
        if (StringUtils.isNotBlank(outputPathStr)) {
            File outputFile = new File(outputPathStr);
            if (outputFile.exists()) {
                outputPath = outputFile.toPath();
            }
        }
        if (directoryManager == null) {
            synchronized (DirectoryManager.class){
                try {
                    if (directoryManager == null){
                        directoryManager = new DirectoryManager(new DirectoryParams(outputPath), ScanRun.createDefault());
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        return directoryManager;
    }



    /**
     * 将DirectoryManager 对象销毁
     */
    public static void destroyDirectoryManager(){
        if (directoryManager != null){
            directoryManager = null;
        }
    }


    public File getUserHome() {
        return this.userHome;
    }


    public File getGetToolPath() {
        if (BooleanUtils.isFalse(getToolPath.exists())) {
            getToolPath.mkdirs();
        }
        return getToolPath;
    }

    public File getRunsOutputDirectory() {
        return this.getOutputDirectory(DirectoryManager.OutputDirectory.RUNS);
    }

    public File getLogOutputDirectory() {
        return this.getRunDirectory(DirectoryManager.RunDirectory.LOG);
    }

    public File getDecompressionDirectory() {
        return this.getRunDirectory(RunDirectory.DECOMPRESSION);
    }


    public File getExtractionsOutputDirectory() {
        return this.getRunDirectory(DirectoryManager.RunDirectory.EXTRACTION);
    }

    public File getRunHomeDirectory() {
        return this.runDirectory;
    }

    private File getOutputDirectory(DirectoryManager.OutputDirectory directory) {
        File actualDirectory = this.outputDirectories.get(directory);
        if (!actualDirectory.exists()) {
            actualDirectory.mkdirs();
        }

        return actualDirectory;
    }

    private File getRunDirectory(DirectoryManager.RunDirectory directory) {
        File actualDirectory = this.runDirectories.get(directory);
        if (!actualDirectory.exists()) {
            actualDirectory.mkdirs();
        }

        return actualDirectory;
    }

    public File getSharedDirectory(String name) {
        File newSharedFile = new File(this.getRunDirectory(DirectoryManager.RunDirectory.SHARED), name);
        newSharedFile.mkdirs();
        return newSharedFile;
    }

    public File getSharedFile(String sharedDirectory, String fileName) {
        return new File(this.getSharedDirectory(sharedDirectory), fileName);
    }


    public enum RunDirectory {
        EXTRACTION("extractions"),
        LOG("logs"),
        SHARED("shared"),
        DECOMPRESSION("decompression");

        private final String dirName;

        RunDirectory(String dirName) {
            this.dirName = dirName;
        }

        public String getDirectoryName() {
            return dirName;
        }
    }

    private enum OutputDirectory {
        RUNS, TOOLS;

        public String getDirectoryName() {
            return name().toLowerCase(); // 将枚举常量转换为小写
        }
    }

}


