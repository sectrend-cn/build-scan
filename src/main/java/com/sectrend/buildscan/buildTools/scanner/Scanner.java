/*
 * Copyright (C) 2018-2020 CLEANSOURCE
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.sectrend.buildscan.buildTools.scanner;

import cn.hutool.core.lang.Pair;
import com.sectrend.buildscan.buildTools.scanner.model.BinaryFilterParam;
import com.sectrend.buildscan.buildTools.scanner.model.ExtWfpHashInfo;
import com.sectrend.buildscan.compress.CompressExtractor;
import com.sectrend.buildscan.constant.ThreadPoolConstants;
import com.sectrend.buildscan.enums.TaskType;
import com.sectrend.buildscan.model.FilePathCollect;
import com.sectrend.buildscan.model.NewScanInfo;
import com.sectrend.buildscan.utils.ThreadPoolUtils;
import com.sectrend.buildscan.utils.WalkFileTreeUtils;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Scanner {

    private static final Logger log = LoggerFactory.getLogger(Scanner.class);
    private static final String BLACKLIST_OPT = "blacklist";
    private static final String IDENTIFY_OPT = "identify";
    private static final String IGNORE_OPT = "ignore";
    private static final String TMP_SCAN_WFP = "/tmp/scan.wfp";

    private final ScannerConf scannerConf;

    private static final String FINGERPRINT_FILE_NAME = "fingerprint_file";

    private static final String WFP_INFO_FILE_NAME = "wfp_info_file";


    public Scanner(ScannerConf scannerConf) {
        super();
        this.scannerConf = scannerConf;

    }


    /**
     * Scans a file
     *
     * @param filename path to the file to be scanned
     * @param scanType Type of scan, leave empty for default.
     * @param sbomPath Optional path to a valid SBOM.json file
     * @param format   Format of the scan. Leave empty for default value.
     * @return an InputStream with the response body
     * @throws IOException
     * @throws InterruptedException
     */
    public InputStream scanFile(String filename, ScanType scanType, String sbomPath, ScanFormat format)
            throws Exception, InterruptedException {
       /* String wfpString = Winnowing.wfpForFile(filename, filename);
        if (wfpString != null && !wfpString.isEmpty()) {
            FileUtils.writeStringToFile(new File(TMP_SCAN_WFP), wfpString, StandardCharsets.UTF_8);
            ScanDetails details = new ScanDetails(TMP_SCAN_WFP, scanType, sbomPath, format);
            return doScan(details);
        }*/
        return null;
    }

    /**
     * Scans a directory and saves the result to a file or prints to STDOUT.
     *
     * @param dir     Directory to scan
     *                //     * @param scanType Type of scan, leave empty for default.
     *                //     * @param sbomPath Optional path to a valid SBOM.json file
     *                //     * @param format   Format of the scan. Leave empty for default value.
     * @param outfile Output file, empty for output to STDOUT
     * @throws IOException
     * @throws InterruptedException
     */
    public File scanDirectory(BinaryFilterParam binaryFilterParam, String dir, String outfile, String wfpKernelThreadSize, String wfpMaxThreadSize, String wfpQueueCapacity, Boolean hpsm, boolean formatEnable) throws Exception {
        //代码运行开始时间
        Long startTime = System.currentTimeMillis();

        File sourceFile = new File(dir);
        String canonicalPath = sourceFile.getCanonicalPath();
        String name = sourceFile.getName();
        log.info("扫描目录:" + dir);
        StringBuilder wfp = new StringBuilder();
        File[] files = sourceFile.listFiles();
        List<FutureTask<StringBuilder>> list = new ArrayList<>();
        //创建线程对象
        ThreadPoolUtils wfpThreadUtils = new ThreadPoolUtils();
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) wfpThreadUtils.createExecutor(wfpKernelThreadSize, wfpMaxThreadSize, wfpQueueCapacity, ThreadPoolConstants.WFP_THREAD_NAME_PREFIX);
        for (File file : files) {
            if (file.isDirectory()) {
                if (!BlacklistRules.filteredDirs(file.getName())) {
                    File[] fileLists = file.listFiles();
                    for (File fileList1 : fileLists) {
                        FutureTask<StringBuilder> f1 = (FutureTask<StringBuilder>) pool.submit(new WfpThread(fileList1.getAbsolutePath(), sourceFile.getAbsolutePath(), hpsm));
                        list.add(f1);
                    }
                }
            } else {
                Files.walkFileTree(Paths.get(file.toString()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (!Files.isDirectory(file) /*&& !BlacklistRules.hasBlacklistedExt(file.toString()) */ /*!BlacklistRules.filteredFiles(file.getFileName().toString()*/) {
                            try {
                                File projectDir = new File(file.toString());
                                String projectPath = projectDir.getAbsolutePath();
                                String wfpString = Winnowing.wfpForFile(binaryFilterParam, name + projectPath.replace(sourceFile.getAbsolutePath(), ""), file.toString(), hpsm, formatEnable);
                                if (wfpString != null && !wfpString.isEmpty())
                                    wfp.append(wfpString);
                            } catch (Exception e) {
                                log.warn("Exception while creating wfp for file: {}", file, e.fillInStackTrace());
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }

        for (FutureTask<StringBuilder> sb : list) {
            wfp.append(sb.get());
        }
        pool.shutdown();

        //代码运行结束时间
        Long endTime = System.currentTimeMillis();
        //计算并打印耗时
        Long tempTime = (endTime - startTime);

        int i = (int) (tempTime / 1000 / 60);
        log.debug("共消耗:" + i + "分钟；共消耗：" + (i * 60) + "秒; 毫秒：" + tempTime);
        File file = new File(outfile + File.separator + name + ".wfp");

        if (file.exists()) {
            file.delete();
        }
        //log.info("生成目录:" + outfile);

        FileUtils.writeStringToFile(file, wfp.toString(), StandardCharsets.UTF_8);
        log.info("生成的指纹文件路径为：{}", file.getAbsolutePath());
        return file;
    }


    public File optimizationScanDirectory(NewScanInfo newScanInfo, FilePathCollect filePathCollect, BinaryFilterParam binaryFilterParam) throws Exception {
        String scanDir = getScanDir(newScanInfo);
        if (StringUtils.isEmpty(scanDir) || !new File(scanDir).exists()) {
            log.error("Scan directory does not exist.");
            System.exit(1);
        }
        File sourceFile = new File(scanDir);
        String name = sourceFile.getName();
        List<String> compressedFiles = Lists.newArrayList();
        log.info("scanning directory：" + scanDir);
        List<String> fileList = WalkFileTreeUtils.walkFileTreeList(scanDir, filePathCollect, newScanInfo.getExcludePaths(), false, newScanInfo.getDefaultParamInfo().getIsUnzip(), compressedFiles);
        log.info("file size ：" + fileList.size());
        if (CollectionUtils.isEmpty(fileList)) {
            log.warn("Scan directory is empty after filter, scan end !");
            System.exit(0);
        }
        File outFile = new File(newScanInfo.getToPath() + File.separator + name + ".wfp");
        if (outFile.exists()) {
            outFile.delete();
        }
        ThreadPoolUtils wfpThreadUtils = new ThreadPoolUtils();
        ThreadPoolTaskExecutor wfpThreadPool = (ThreadPoolTaskExecutor) wfpThreadUtils.createExecutor(newScanInfo.getWfpKernelThreadSize(), newScanInfo.getWfpMaxThreadSize(), newScanInfo.getWfpQueueCapacity(), ThreadPoolConstants.WFP_THREAD_NAME_PREFIX);

        String grandParentDir = sourceFile.getParent();
        if (!grandParentDir.endsWith( File.separator )) {
            grandParentDir += File.separator;
        }
        log.info("The path of the generated fingerprint file is：{}", outFile.getAbsolutePath());
        FileUtils.writeStringToFile(outFile, "", StandardCharsets.UTF_8);
        log.info("Scan files in the directory to generate fingerprints!");
        createWfp(binaryFilterParam, fileList, grandParentDir, newScanInfo.getHpsm(), outFile, wfpThreadPool, newScanInfo.isFormatEnable());
        if (CollectionUtils.isNotEmpty(compressedFiles)) {
            List<String> decompressionFileList = Collections.synchronizedList(new ArrayList<>());

            DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
            String decompressionOutputPath = directoryManager.getDecompressionDirectory().getAbsolutePath() + File.separator + sourceFile.getName();
            File decompressionDirectory = directoryManager.getDecompressionDirectory();
            newScanInfo.setDecompressionDirectory(decompressionOutputPath);
            newScanInfo.setDecompressionParentDirectory(directoryManager.getDecompressionDirectory().getAbsolutePath() + File.separator);
            ThreadPoolUtils threadPoolUtils = new ThreadPoolUtils();
            ThreadPoolTaskExecutor compressedThreadPool = (ThreadPoolTaskExecutor)threadPoolUtils.createExecutor("5", "10", "1000", ThreadPoolConstants.DECOMPRESS_THREAD_NAME_PREFIX);
            log.info("Start decompressing compressed files!");
            CompressExtractor.decompressSubFiles(scanDir, filePathCollect, newScanInfo.getExcludePaths(), compressedFiles, decompressionOutputPath, compressedThreadPool, decompressionFileList);
            log.info("End of decompression and compression of files");
            if (CollectionUtils.isNotEmpty(decompressionFileList)) {
                log.info("Extract the file and start generating fingerprints");
                createWfp(binaryFilterParam, decompressionFileList, decompressionDirectory.getAbsolutePath() + File.separator, newScanInfo.getHpsm(), outFile, wfpThreadPool, newScanInfo.isFormatEnable());
                log.info("End of extracting files and generating fingerprints");
            }
            compressedThreadPool.shutdown();
        }
        wfpThreadPool.shutdown();
        return outFile;
    }

    @Nullable
    private String getScanDir(NewScanInfo newScanInfo) {
        String scanDir = null;
        if (TaskType.FINGER_TASK_TYPE.getValue().equals(newScanInfo.getTaskType())) {
            scanDir = newScanInfo.getFromPath();
        } else if(TaskType.TASK_SCAN_TYPE.getValue().equals(newScanInfo.getTaskType())) {
            scanDir = newScanInfo.getTaskDir();
        }
        return scanDir;
    }


    private void createWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String sourceFile, Boolean hpsm, File outputFile, ThreadPoolTaskExecutor pool, boolean formatEnable) throws IOException, InterruptedException {
        Long startTime = System.currentTimeMillis();
        CountDownLatch countDownLatch = new CountDownLatch(fileList.size());
        ReentrantLock writeFileLock = new ReentrantLock();

        for (String fileItem : fileList) {
            pool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String wfpStr = generateWfp(binaryFilterParam, new File(fileItem), sourceFile, hpsm, formatEnable);
                        writeFileLock.lock();
                        try {
                            write(outputFile.getAbsolutePath(), wfpStr);
                        } catch (Throwable t) {
                            log.warn("Writing task data {} exception", outputFile.getPath(), t);
                        } finally {
                            writeFileLock.unlock();
                        }
                    } catch (Throwable t) {
                        log.warn("executing task {} exception", outputFile.getPath(), t);
                    } finally {
                        countDownLatch.countDown();

                    }
                }
            });
        }
        countDownLatch.await();
        Long endTime = System.currentTimeMillis();
        Long tempTime = endTime - startTime;
        long i = tempTime / 1000L / 60L;
        log.info("file destination " + sourceFile + ", Generate fingerprint file total consumption：" + i + " Minutes; consumption ：" + i * 60 + " Second; millisecond：" + tempTime);
    }


    public File createWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String outfile, String rootPath, Boolean hpsm, String wfpKernelThreadSize, String wfpMaxThreadSize, String wfpQueueCapacity, Integer index, boolean formatEnable) throws Exception {
        ThreadPoolUtils wfpThreadUtils = new ThreadPoolUtils();
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) wfpThreadUtils.createExecutor(wfpKernelThreadSize, wfpMaxThreadSize, wfpQueueCapacity, ThreadPoolConstants.WFP_THREAD_NAME_PREFIX);
        File wfpFile = createWfp(binaryFilterParam, fileList, outfile, rootPath, hpsm, index, pool, formatEnable);
        pool.shutdown();
        return wfpFile;
    }


    public File createWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String outputFileDir, String rootPath, Boolean hpsm, Integer index, ThreadPoolTaskExecutor threadPoolTaskExecutor, boolean formatEnable) throws Exception {

        File outputFile;
        if (Objects.nonNull(index)) {
            outputFile = new File(outputFileDir + File.separator + FINGERPRINT_FILE_NAME + "_" + index + ".wfp");
        } else {
            outputFile = new File(outputFileDir + File.separator + FINGERPRINT_FILE_NAME + ".wfp");
        }
        if (outputFile.exists()) {
            outputFile.delete();
        }
        log.info("Generate directory：" + outputFileDir);
        log.info("The path of the generated fingerprint file is：{}", outputFile.getAbsolutePath());

        FileUtils.writeStringToFile(outputFile, "", StandardCharsets.UTF_8);
        createWfp(binaryFilterParam, fileList, rootPath, hpsm, outputFile, threadPoolTaskExecutor, formatEnable);
        return outputFile;
    }



    public Pair<File, List<ExtWfpHashInfo>> createWfp(BinaryFilterParam binaryFilterParam, List<String> fileList, String outfile, String rootPath, Boolean hpsm, Integer index, ThreadPoolTaskExecutor threadPoolTaskExecutor, Boolean unzipArchives, Map<String, String> softWarePackagePathMap, Integer batchSize, boolean formatEnable) throws Exception {
        Long startTime = Long.valueOf(System.currentTimeMillis());
        log.info("file size ：" + fileList.size());
        int batch = fileList.size() / batchSize + (fileList.size() % batchSize == 0 ? 0 : 1);
        List<CompletableFuture> arrayOfCompletableFuture = new ArrayList<>();
        List<ExtWfpHashInfo> wfpHashInfos = new ArrayList<>();

        for (int i = 0; i < batch; i++) {
            List<String> subList = fileList.subList(i * batchSize, i == batch - 1 ? fileList.size() : batchSize * (i + 1));
            ExtWfpHashInfo wfpHashInfo = new ExtWfpHashInfo();
            wfpHashInfos.add(wfpHashInfo);
            arrayOfCompletableFuture.addAll(subList.stream().map(
                            file -> CompletableFuture.supplyAsync(() -> generateWfp(binaryFilterParam, new File(file), rootPath, hpsm, unzipArchives, softWarePackagePathMap, wfpHashInfo, formatEnable), threadPoolTaskExecutor).exceptionally(ex -> {
                                log.warn("Task execution exception：" + ex.getMessage());
                                log.warn("Exception file：" + file);
                                return Strings.EMPTY;
                            })).filter(o -> Objects.nonNull(o))
                    .collect(Collectors.toList()));
        }
        CompletableFuture.allOf(arrayOfCompletableFuture.toArray(new CompletableFuture[fileList.size()])).join();
        File file;
        if (Objects.nonNull(index)) {
            file = new File(outfile + File.separator + FINGERPRINT_FILE_NAME + "_" + index + ".wfp");
        } else {
            file = new File(outfile + File.separator + FINGERPRINT_FILE_NAME + ".wfp");
        }
        if (file.exists()) {
            file.delete();
        }
        log.info("Generate directory：" + outfile);
        log.info("The path of the generated fingerprint file is：{}", file.getAbsolutePath());
        if (!CollectionUtils.isEmpty(arrayOfCompletableFuture)) {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file.getAbsolutePath(), true));) {
                for (CompletableFuture<String> completableFuture : arrayOfCompletableFuture) {
                    String wfpR = completableFuture.get();
                    if (StringUtils.isNotBlank(wfpR)) {
                        bufferedWriter.write(wfpR);
                    }
                }
            } catch (Throwable t) {
                log.error("failed to write wfp {}", file.getAbsolutePath(), t);
            }
        } else {
            FileUtils.writeStringToFile(file, "", StandardCharsets.UTF_8);
        }


        Long endTime = Long.valueOf(System.currentTimeMillis());
        Long tempTime = Long.valueOf(endTime.longValue() - startTime.longValue());
        int i = (int) (tempTime.longValue() / 1000L / 60L);
        log.debug("file destination " + rootPath + ", Generate fingerprint file total consumption：" + i + "Minutes; consumption ：" + i * 60 + "Second; millisecond：" + tempTime);

        return new Pair<>(file, wfpHashInfos);
    }



    public static String generateWfp(BinaryFilterParam binaryFilterParam, File file, String name, File sourceFile, Boolean hpsm, boolean formatEnable) {
        try {
            File projectDir = new File(file.toString());
            String projectPath = projectDir.getAbsolutePath();
            String s = Winnowing.wfpForFile(binaryFilterParam, name + projectPath.replace(sourceFile.getAbsolutePath(), ""), file.toString(), hpsm, formatEnable);
            return s.replaceAll("\\\\", "/");
        } catch (Exception e) {
            log.warn("Exception while creating wfp for file: {}", file, e);
        }
        return Strings.EMPTY;
    }


    public static String generateWfp(BinaryFilterParam binaryFilterParam, File file, String rootFilePath, Boolean hpsm, boolean formatEnable) {
        try {
            String projectFilePath = file.getAbsolutePath();

            return Winnowing.wfpForFile(binaryFilterParam, projectFilePath.substring(rootFilePath.length(), projectFilePath.length()), file.toString(), hpsm, formatEnable);
        } catch (Exception e) {
            log.warn("Exception while creating wfp for file: {}", file, e);
        }
        return null;
    }

    public static String generateWfp(BinaryFilterParam binaryFilterParam, File file, String rootFilePath, Boolean hpsm, Boolean unzipArchives, Map<String, String> softWarePackagePathMap, ExtWfpHashInfo wfpHashInfo, boolean formatEnable) {
        try {
            String projectFilePath = file.getAbsolutePath();

            return Winnowing.wfpForFile(binaryFilterParam, projectFilePath.substring(rootFilePath.length(), projectFilePath.length()), file.toString(), hpsm, unzipArchives, softWarePackagePathMap, wfpHashInfo, formatEnable);
        } catch (Exception e) {
            log.warn("Exception while creating wfp for file: {}", file, e);
        }
        return null;
    }



    public static void write(String filepath, String text) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(filepath, true), StandardCharsets.UTF_8));
            try {
                bufferedWriter.write(text);
                bufferedWriter.close();
            } catch (Throwable throwable) {
                try {
                    bufferedWriter.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Scans a file and either saves it to a file or prints to STDOUT
     *
     * @param filename path to the file to be scanned
     * @param scanType Type of scan, leave empty for default.
     * @param sbomPath Optional path to a valid SBOM.json file
     * @param format   Format of the scan. Leave empty for default value.
     * @param outfile  Output file, empty for output to STDOUT
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws InterruptedException
     */
    public void scanFileAndSave(String filename, ScanType scanType, String sbomPath, ScanFormat format, String
            outfile)
            throws Exception {

        InputStream inputStream = scanFile(filename, scanType, sbomPath, format);
        if (inputStream != null) {
            OutputStream out = StringUtils.isEmpty(outfile) ? System.out : new FileOutputStream(outfile);
            IOUtils.copy(inputStream, out);
            inputStream.close();
            if (!StringUtils.isEmpty(outfile))
                out.close();
        }
    }


    private static Map<Object, Object> scanFormData(ScanDetails details) throws IOException {
        Map<Object, Object> data = new HashMap<>();
        if (details.getScanType() != null && StringUtils.isNotEmpty(details.getSbomPath())) {
            String sbomContents = FileUtils.readFileToString(new File(details.getSbomPath()), StandardCharsets.UTF_8);
            data.put("type", details.getScanType());
            data.put("assets", sbomContents);
        }
        if (details.getFormat() != null) {
            data.put("format", details.getFormat());
        }

        Path wfPath = Paths.get(details.getWfp());
        data.put("file", wfPath);
        return data;
    }


    public String createSnippetWfp(String snippet) {
        return  Winnowing.getWfpForSnippet(snippet, true);

    }

}
