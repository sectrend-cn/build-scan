package com.sectrend.buildscan.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.scanner.Scanner;
import com.sectrend.buildscan.buildTools.scanner.ScannerConf;
import com.sectrend.buildscan.buildTools.scanner.model.BinaryFilterParam;
import com.sectrend.buildscan.buildTools.scanner.model.ExtWfpHashInfo;
import com.sectrend.buildscan.model.DependencyRoot;
import com.sectrend.buildscan.model.NewScanInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.hutool.core.lang.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReferenceClient {


    /**
     * 获取并生成指纹
     * @param fileList
     * @param toPath
     * @param rootPath
     * @param index
     * @param hpsm
     * @return
     */
    public static File getWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String toPath, String rootPath, Boolean hpsm, String wfpKernelThreadSize, String wfpMaxThreadSize, String wfpQueueCapacity, Integer index, boolean formatEnable) {

        String overrideAPIURL = System.getenv("CLEAN_SOURCE_API");
        String overrideAPIKEY = System.getenv("CLEAN_SOURCE_API");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);

        try {
            log.info("--------in process of create wfp --------");
            File wfp = scanner.createWfp(binaryFilterParam, fileList, toPath, rootPath, hpsm, wfpKernelThreadSize, wfpMaxThreadSize, wfpQueueCapacity, index, formatEnable);
            log.info("--------create wfp end--------");

            return wfp;
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.error("--------create wfp error--------{}", Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    public static String getWfp(String snippet) {
        String overrideAPIURL = System.getenv("CLEAN_SOURCE_API");
        String overrideAPIKEY = System.getenv("CLEAN_SOURCE_API");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);

        try {
            log.info("--------in process of create wfp --------");
            String wfp = scanner.createSnippetWfp(snippet);
            log.info("--------create wfp end--------");
            return wfp;
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.error("--------create wfp error--------{}", Arrays.toString(e.getStackTrace()));
        }
        return null;
    }


    public static File getWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String toPath, String rootPath, Boolean hpsm, Integer index, ThreadPoolTaskExecutor threadPoolTaskExecutor, boolean formatEnable) {

        String overrideAPIURL = System.getenv("CLEAN_SOURCE_API");
        String overrideAPIKEY = System.getenv("CLEAN_SOURCE_API");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);

        try {
            log.info("--------in process of create wfp --------");
            File wfp = scanner.createWfp(binaryFilterParam, fileList, toPath, rootPath, hpsm, index, threadPoolTaskExecutor, formatEnable);
            log.info("--------create wfp end--------");

            return wfp;
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.error("--------create wfp error--------{}", Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    public static Pair<File, List<ExtWfpHashInfo>> getWfp(List<String> fileList, Integer binaryFlag, List<String> binaryScanPathList, String toPath, String rootPath, Boolean hpsm, Integer index, ThreadPoolTaskExecutor threadPoolTaskExecutor, Boolean unzipArchives, Map<String, String> softWarePackagePathMap, Integer batchSize, boolean formatEnable) {

        String overrideAPIURL = System.getenv("CLEAN_SOURCE_API");
        String overrideAPIKEY = System.getenv("CLEAN_SOURCE_API");
        ScannerConf conf = ScannerConf.defaultConf();
        if (StringUtils.isNotEmpty(overrideAPIURL)) {
            conf = new ScannerConf(overrideAPIURL, overrideAPIKEY);
        }
        Scanner scanner = new Scanner(conf);

        BinaryFilterParam binaryFilterParam = new BinaryFilterParam();
        binaryFilterParam.setMixedBinaryScanFlag(binaryFlag);
        binaryFilterParam.setMixedBinaryScanFilePathList(binaryScanPathList);

        try {
            log.info("--------in process of create wfp --------");
            Pair<File, List<ExtWfpHashInfo>> wfp = scanner.createWfp(binaryFilterParam, fileList, toPath, rootPath, hpsm, index, threadPoolTaskExecutor, unzipArchives, softWarePackagePathMap, batchSize, formatEnable);
            log.info("--------create wfp end--------");
            return wfp;
        } catch (Exception e) {
            log.debug(e.getMessage());
            log.error("--------create wfp error--------{}", Arrays.toString(e.getStackTrace()));
        }
        return null;
    }

    public static List<DependencyRoot> getBuildDependencyRoot(String dir) {

        ScannableEnvironment scannableEnvironment = new ScannableEnvironment(new File(dir), "");

        DependencyRoot dependencyRoot = new DependencyRoot();
        //返回构建扫描的数据集
        List<DependencyRoot> resultList = new ArrayList<>();
        NewScanInfo newScanInfo = new NewScanInfo();
        newScanInfo.setTaskFileDir(dir);
        try {
            resultList = BuildDependencyUtil.buildScan(scannableEnvironment, newScanInfo, null);
        } catch (Exception e) {
            log.error(e.getLocalizedMessage());
        }

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        log.info("build result: " + gson.toJson(resultList));
        return resultList;
    }


}
