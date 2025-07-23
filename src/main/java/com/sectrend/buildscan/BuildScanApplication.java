package com.sectrend.buildscan;


import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.scanner.model.BinaryFilterParam;
import com.sectrend.buildscan.enums.AuthType;
import com.sectrend.buildscan.enums.ScanType;
import com.sectrend.buildscan.enums.TaskType;
import com.sectrend.buildscan.model.*;
import com.sectrend.buildscan.result.RemotingClient;
import com.sectrend.buildscan.utils.BuildDependencyUtil;
import com.sectrend.buildscan.utils.CheckUtils;
import com.sectrend.buildscan.utils.ZipUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@EnableAsync
public class BuildScanApplication implements ApplicationRunner {
    private final Logger logger = LoggerFactory.getLogger(BuildScanApplication.class);

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(BuildScanApplication.class);
        builder.logStartupInfo(false);
        builder.run(args);
    }

    @Override
    public void run(ApplicationArguments args) {

        //动态日志级别
        RemotingClient.initLog(args);
        //初始化入参
        NewScanInfo newScanInfo = RemotingClient.initNewScanInfo(args);

        logger.info("-----        Detect Version CleanSource_SCA: 4.0.0        -----");
        logger.info("-------------START OF SCAN------------");
        //打印入参日志
        RemotingClient.printParamLog(newScanInfo);

        BinaryFilterParam binaryFilterParam = new BinaryFilterParam();

        //判断是否是单独生成指纹文件
        if (TaskType.TASK_SCAN_TYPE.getValue().equals(newScanInfo.getTaskType())) {
            //参数校验
            verifyParam(newScanInfo);

            setToPath(newScanInfo, newScanInfo.getTaskDir());

            if (ScanType.SOURCE_SCAN_TYPE.getValue().equals(newScanInfo.getScanType())) {
                List<File> scanResultFiles = Lists.newArrayList();
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        deleteFiles(scanResultFiles);
                    }
                });
                File taskScanDirectory = new File(newScanInfo.getTaskDir());
                if (!taskScanDirectory.exists() || FileUtil.isDirEmpty(taskScanDirectory)) {
                    logger.warn("Scan directory is empty, scan end !");
                    System.exit(0);
                }

                Long taskDirSize = calculateDirSize(taskScanDirectory);
                logger.info("扫描的源码目录绝对路径:{},扫描的源码目录大小:{}", taskScanDirectory.getAbsolutePath(), taskDirSize);

                //校验源码路径
                CheckUtils.checkLocationAndType(newScanInfo);
                String mappingPath = newScanInfo.getToPath() + File.separator + "compressMapping" + ".json";
                FilePathCollect filePathCollect = new FilePathCollect();

                binaryFilterParam.setMixedBinaryScanFlag(newScanInfo.getDefaultParamInfo().getMixedBinaryScanFlag());
                binaryFilterParam.setMixedBinaryScanFilePathList(newScanInfo.getDefaultParamInfo().getMixedBinaryScanFilePaths());

                //生成指纹文件
                Boolean wfpFlag = RemotingClient.generateWfpFile(newScanInfo, mappingPath, filePathCollect, binaryFilterParam);
                scanResultFiles.add(RemotingClient.getWfpFile());
                if (BooleanUtils.isFalse(wfpFlag)) {
                    System.exit(1);
                }
                newScanInfo.setMappingPath(mappingPath);
                scanResultFiles.add(new File(mappingPath));
                ScannableEnvironment scannableEnvironment = new ScannableEnvironment(taskScanDirectory, newScanInfo.getBuildTreeFile());

                File zipUrl = null;
                File binaryZipUrl = null;
                Set<String> projectLicenseSet = new HashSet<>();
                //文件是否授权，授权则压缩
                if (1 == newScanInfo.getDefaultParamInfo().getIsSaveSourceFile()) {
                    //压缩排除文件第一层文件夹
                    logger.info("compressed files start");
                    zipUrl = ZipUtils.getZipFile(scannableEnvironment.getDirectory(), newScanInfo.getToPath());
                    scanResultFiles.add(zipUrl);
                    logger.info("compressed files end");
                } else {
                    if (newScanInfo.getDefaultParamInfo().getMixedBinaryScanFlag() == 1 && CollectionUtils.isNotEmpty(binaryFilterParam.getBinaryScanList())) {
                        // 如果开启了二进制扫描开关，即使没有授权存储源文件，也需要将二进制文件压缩上传至服务端
                        binaryZipUrl = ZipUtils.getZipFileByList(binaryFilterParam.getBinaryRealScanList(), newScanInfo.getToPath());
                        scanResultFiles.add(binaryZipUrl);
                    }
                    if (CollectionUtils.isNotEmpty(filePathCollect.getProjectLicenseFile()) && StringUtils.isBlank(newScanInfo.getLicenseName())) {
                        //分析项目许可证
                        for (String licenseFile : filePathCollect.getProjectLicenseFile()) {
                            String projectLicense = RemotingClient.analyseLicense(licenseFile);
                            if (StringUtils.isNotBlank(projectLicense)) {
                                projectLicenseSet.add(projectLicense);
                            }
                        }
                        if (CollectionUtils.isNotEmpty(projectLicenseSet)) {
                            newScanInfo.setLicenseName(String.join("----", projectLicenseSet));
                        }
                    }
                }
                //生成项目名称版本号信息
                if (!(StringUtils.isNotBlank(newScanInfo.getCustomProject()) && StringUtils.isNotBlank(newScanInfo.getCustomProduct()) && StringUtils.isNotBlank(newScanInfo.getCustomVersion()))) {
                    logger.info("build project info start");
                    BuildDependencyUtil.buildProjectInfo(newScanInfo, scannableEnvironment);
                    logger.info("build project info end");
                }
                //生成构建信息
                logger.info("build dependency info start");
                List<DependencyRoot> dependencyRoots = BuildDependencyUtil.buildDependencyRoot(newScanInfo, scannableEnvironment, filePathCollect);
                logger.info("build dependency info end");
                //扫描数据
                String buildPath = "";
                if (newScanInfo.getBuildDepend() != null && newScanInfo.getBuildDepend()) {
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    String scanResult = gson.toJson(dependencyRoots);
                    //生成构建文件
                    buildPath = newScanInfo.getToPath() + File.separator + taskScanDirectory.getName() + ".json";
                    //写入文件
                    RemotingClient.writeFile(scanResult, buildPath);
                    scanResultFiles.add(new File(buildPath));
                }
                //上传后台数据
                logger.info("create a task start");
                Boolean aBoolean = RemotingClient.uploadData(zipUrl, RemotingClient.getWfpFile(), buildPath, newScanInfo, taskDirSize);
                logger.info("create a task end");

                System.out.println("------------- END OF SCAN ------------");
                if (!aBoolean) {
                    System.exit(2);
                } else {
                    System.exit(0);
                }

            } else if (ScanType.DOCKER_SCAN_TYPE.getValue().equals(newScanInfo.getScanType())) {
                CheckUtils.checkTaskFileDir(newScanInfo);
                //上传docker文件
                logger.info("upload docker file start");
                Boolean aBoolean = RemotingClient.uploadAndCreateTask(newScanInfo);
                logger.info("------------- END OF SCAN ------------");
                if (!aBoolean) {
                    System.exit(2);
                } else {
                    System.exit(0);
                }
            } else if (ScanType.BINARY_SCAN_TYPE.getValue().equals(newScanInfo.getScanType())) {
                CheckUtils.checkTaskFileDir(newScanInfo);
                //上传binary文件
                logger.info("upload binary file start");
                Boolean aBoolean = RemotingClient.uploadAndCreateTask(newScanInfo);
                logger.info("------------- END OF SCAN ------------");
                if (!aBoolean) {
                    System.exit(2);
                } else {
                    System.exit(0);
                }
            } else if ("upload_test".equals(newScanInfo.getScanType())) {
                File zipUrl = StringUtils.isNotBlank(newScanInfo.getTaskDir()) ? new File(newScanInfo.getTaskDir()) : null;
                try {
                    logger.info("Uploading test file -|");
                    RemotingClient.uploadFile(newScanInfo, zipUrl, new FileTempDto());
                    logger.info("Uploading test file completed -|");
                } catch (IOException e) {
                    logger.error("Exception occurred while uploading test file", e);
                }
                System.exit(0);
            } else {
                logger.error("operation failure：scan type not found");
                System.exit(3);
            }
        } else {
            logger.error("operation failure：task type not found");
            System.exit(4);
        }
    }

    private Long calculateDirSize(File rootDir) {
        if (rootDir == null || !rootDir.exists()) return 0L;

        AtomicLong totalSize = new AtomicLong(0);
        ConcurrentLinkedQueue<File> dirQueue = new ConcurrentLinkedQueue<>();
        dirQueue.add(rootDir);

        int threadCount = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                File dir;
                while ((dir = dirQueue.poll()) != null) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                totalSize.addAndGet(file.length());
                            } else if (file.isDirectory()) {
                                dirQueue.add(file);
                            }
                        }
                    }
                }
            }));
        }

        // 等待所有线程完成
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception ignored) {
            }
        }
        pool.shutdown();
        return totalSize.get();
    }

    private File buildBinaryParamFile(BinaryFilterParam binaryFilterParam, String filePath) {
        File localFile = new File(filePath);

        try (BufferedWriter resultWriter = new BufferedWriter(new FileWriter(localFile), 1 * 1024)) {
            resultWriter.write(JSON.toJSONString(binaryFilterParam));
            resultWriter.flush();
        } catch (Throwable t) {
            logger.error("build binary param file failure", t);
            return null;
        }
        return localFile;
    }

    /**
     * 设置输出路径
     *
     * @param newScanInfo 扫描参数对象
     * @param scanPath    扫描路径
     */
    private void setToPath(NewScanInfo newScanInfo, String scanPath) {
        File defaultFile;
        if (StringUtils.isBlank(newScanInfo.getToPath())) {
            /*int lastSeparatorIndex = scanPath.lastIndexOf(File.separator);
            String defaultPath = "";
            if (lastSeparatorIndex >= 0) {
                defaultPath = scanPath.substring(0, lastSeparatorIndex);
            }*/
            defaultFile = new File(scanPath);
            newScanInfo.setToPath(defaultFile.getParent());
        } else {
            defaultFile = new File(newScanInfo.getToPath());
            newScanInfo.setToPath(defaultFile.getAbsolutePath());
        }
    }


    /**
     * 删除扫描结构文件集合
     */
    private void deleteFiles(List<File> scanResultFiles) {
        if (CollectionUtils.isNotEmpty(scanResultFiles)) {
            System.gc();
            scanResultFiles.forEach(scanResultFile -> {
                if (scanResultFile != null && scanResultFile.exists()) {
                    try {
                        logger.info("Deleted the file: " + scanResultFile.getName());
                        FileUtil.del(scanResultFile);
                    } catch (Exception e) {
                    }
                }
            });
        }
    }


    private void verifyParam(NewScanInfo newScanInfo){
        //校验登录
        CheckUtils.checkLogin(newScanInfo);

        boolean flag;
        //如果是token调取
        if (StringUtils.isNotBlank(newScanInfo.getToken())) {
            logger.info("verify token start ");
            newScanInfo.setAuthType(AuthType.Token);
            flag = RemotingClient.verifyToken(newScanInfo);
            logger.info("verify token end; {}", flag ? "successed" : "failed");
        } else {
            //登录
            logger.info("verify login start ");
            newScanInfo.setAuthType(AuthType.Cookie);
            flag = RemotingClient.login(newScanInfo);
            logger.info("verify login end; {}", flag ? "successed" : "failed");
        }
        if (BooleanUtils.isFalse(flag)) {
            System.exit(5);
        }

        //判断license是否存在
        if (StringUtils.isNotBlank(newScanInfo.getLicenseName())) {
            logger.info("verify license name start licenseName:{}", newScanInfo.getLicenseName());
            RemotingClient.verifyLicenseUrl(newScanInfo);
            logger.info("verify license name end");
        }

        //验证邮箱
        if (StringUtils.isNotBlank(newScanInfo.getNotificationEmail())) {
            String notificationEmail = newScanInfo.getNotificationEmail();
            String[] split = notificationEmail.split(",");
            String regex = "^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$";
            for (String email : split) {
                boolean matches = email.matches(regex);
                if (!matches) {
                    logger.error("The email format is incorrect：{}", newScanInfo.getNotificationEmail());
                    System.exit(6);
                }
            }

            //调用验证是否绑定邮箱接口
            logger.info("verify email start");
            RemotingClient.verifyEmail(newScanInfo);
            logger.info("verify email end");
        }
        //验证扫描方式
        if (newScanInfo.getDefaultParamInfo() != null) {
            DefaultParamInfo defaultParamInfo = newScanInfo.getDefaultParamInfo();
            if (!(defaultParamInfo.getScanWay() == 1 || defaultParamInfo.getScanWay() == 2)) {
                logger.error("Scan mode parameter error '1' Full scan，'2' fast scan：{}", defaultParamInfo.getScanWay());
            }
        }
        //校验扫描类型
        CheckUtils.checkScanType(newScanInfo);
    }

}


