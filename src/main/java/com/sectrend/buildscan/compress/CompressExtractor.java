package com.sectrend.buildscan.compress;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.sectrend.buildscan.executable.impl.SimpleExecutableRunner;
import com.sectrend.buildscan.model.FilePathCollect;
import com.sectrend.buildscan.model.NewScanInfo;
import com.sectrend.buildscan.utils.WalkFileTreeUtils;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

/**
 * @author yihuishun
 *  解压 压缩文件
 *  目前支持解压提取  "zip" ,"tar" ,"tar.gz", "gz" ,"jar" ,"war" ,"apk", "tgz", "rpm", "cpio", "tar.xz", "xz"
 */
public class CompressExtractor {

    private static final Logger logger = LoggerFactory.getLogger(CompressExtractor.class);

    public static final String DATE_FORMAT = "yyyyMMddHHmmssSSS";

    public static SimpleExecutableRunner simpleExecutableRunner = new SimpleExecutableRunner();

    // 支持解压存档名称集合
    public static final List<String> ARCHIVER_NAME_LIST = Arrays.asList( "zip" ,"tar" ,"tar.gz", "gz" ,"jar" ,"war" ,"apk", "tgz", "rpm", "cpio", "tar.bz2", "bz2", "tar.xz", "xz", "7z", "rar");

    // 存储解压目标文件, 方便后面删除
//    public static final Set<File> TARGET_FILE_SET = new HashSet<>();

    public static final Map<String,String> softWarePackagePathMap = new ConcurrentHashMap<>();

    // 存储压缩文件解压路径映射 (压缩包中如果存在其他压缩包则只存储顶级解压文件目录)
    public static Map<String, String> rootCompressPathMap = new ConcurrentHashMap<>();

    private static int num = 1;

    private static String rootPath;

    private static String rootDir;


    private static File rootFile;




    /**
     * 解压目录下全部压缩文件
     */
    public static void unCompressAll(String sourceCodePath, NewScanInfo newScanInfo) {
        try {

            logger.info("--------Start searching for extracted files and extracting them--------");
            DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
            String decompressionOutputPath = directoryManager.getDecompressionDirectory().getAbsolutePath() + File.separator;
            newScanInfo.setDecompressionDirectory(decompressionOutputPath);
            newScanInfo.setDecompressionParentDirectory(decompressionOutputPath);
            File sourceCodeFile = new File(sourceCodePath);
            findAndUnCompress(sourceCodePath, decompressionOutputPath + sourceCodeFile.getName(), newScanInfo.getExcludePaths());
            logger.info("--------End searching for unzipped files and unzip them--------");
        } finally {
            // 使用ShutdownHook事件 删除解压文件
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    CompressExtractor.removeTargetFile();
                }
            });
        }
    }

    /**
     * 解压目录下全部压缩文件
     */
    public static void unCompressAll(String sourceCodePath) {
        try {

            logger.info("--------Start searching for extracted files and extracting them--------");
            DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
            File sourceCodeFile = new File(sourceCodePath);
            String decompressionOutputPath = directoryManager.getDecompressionDirectory().getAbsolutePath() + File.separator + sourceCodeFile.getName();
            findAndUnCompress(sourceCodePath, decompressionOutputPath);
            logger.info("--------End searching for unzipped files and unzip them--------");
        } finally {
            // 使用ShutdownHook事件 删除解压文件
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    CompressExtractor.removeTargetFile();
                }
            });
        }
    }
    private static void findAndUnCompress(String sourcePath, String decompressionOutputPath, List<String> excludePaths) {
        if (num == 1) {
            rootPath = sourcePath;
            rootFile = new File(sourcePath);
            try {
                String[] split = rootPath.split("[/\\\\]");
                rootDir = StringUtils.isBlank(split[split.length - 1]) ? split[split.length - 2] : split[split.length - 1];
                if (CollectionUtils.isNotEmpty(excludePaths)) {
                    if (excludePaths.stream().anyMatch(s -> Pattern.matches(s, rootDir + "/"))) {
                        return;
                    }
                }
            } catch (Throwable t) {
                logger.error("failed to filter file by {}", JSON.toJSONString(excludePaths), t);
            }
        }
        num ++;
        try {
            File sourceFile = FileUtil.file(sourcePath);
            if (Objects.isNull(sourceFile) || !sourceFile.exists()) {
                return;
            }
            //查找目录下全部文件
            File[] files = new File[]{};
            if(sourceFile.isDirectory()) {
                //查找目录下全部文件
                files = FileUtil.ls(sourcePath);
            }
            //循环便利文件
            for (File file : files){
                // 文件对象是否为空  或者 文件是否是软链接 或者 文件开头为`.` 则结束本次循环
                try {
                    if(null == file || Files.isSymbolicLink(file.toPath()) /*|| file.getName().startsWith(".")*/) {
                        continue;
                    }
                    // 判断是否文件夹  如果是文件夹则递归查找解析
                    if(file.isDirectory()){
                        if(CollectionUtils.isNotEmpty(excludePaths)){
                            String absolutePath = file.getAbsolutePath();
                            String relativePath = null;
                            if (absolutePath.startsWith(rootPath)) {
                                relativePath = absolutePath.substring(rootPath.length());
                            } else {
                                relativePath = absolutePath.substring(decompressionOutputPath.length());
                            }
                            String finalRelativePath = ((rootDir + (relativePath.startsWith(File.separator) ? relativePath : File.separator + relativePath)) + File.separator).replace("\\", "/");
                            if (excludePaths.stream().anyMatch(s -> Pattern.matches(s, finalRelativePath))) {
                                continue;
                            }
                        }
                        findAndUnCompress(file.getAbsolutePath(), decompressionOutputPath, excludePaths);
                        continue;
                    }
                    String archiverName = FileUtil.getType(file);
                    // 校验文件是否是支持得压缩文件
                    if(BooleanUtils.isFalse(ARCHIVER_NAME_LIST.contains(archiverName))) {
                        continue;
                    }
                    String targetDirectory = getTargetDirectory(file, rootPath, decompressionOutputPath);
                    File targetFile = FileUtil.file(targetDirectory);
                    if(targetFile != null){
                        if(CollectionUtils.isNotEmpty(excludePaths)){
                            String absolutePath = file.getAbsolutePath();
                            String relativePath = null;
                            if (absolutePath.startsWith(rootPath)) {
                                relativePath = absolutePath.substring(rootPath.length());
                            } else {
                                relativePath = absolutePath.substring(decompressionOutputPath.length());
                            }
                            String finalRelativePath = ((rootDir + (relativePath.startsWith(File.separator) ? relativePath : File.separator + relativePath))).replace("\\", "/");
                            if (excludePaths.stream().anyMatch(s -> Pattern.matches(s, finalRelativePath))) {
                                return;
                            }
                        }
                        if (!targetFile.exists()) {
                            Files.createDirectories(targetFile.toPath());
                        }
                        if (isAdd(targetFile.getAbsolutePath())) {
//                            TARGET_FILE_SET.add(targetFile);
                            rootCompressPathMap.put(file.getAbsolutePath(), targetFile.getAbsolutePath());
                        }
                        // 判断是否解压成功
                        if(unCompressMain(file, targetFile, archiverName, rootPath) && targetFile.exists()){
                            // 压缩包解压成功后继续循环查找压缩包中得文件  查询是否里面包含其他压缩包
                            findAndUnCompress(targetFile.getAbsolutePath(), decompressionOutputPath, excludePaths);
                        }
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.warn("Decompression failed: " + sourcePath + ". " + e.getMessage(), e);
        }
    }

    @NotNull
    private static String getTargetDirectory(File file, String rootPath, String decompressionOutputPath) {
        String sourceCodeParentPath = file.getParentFile().getAbsolutePath();

        String decompressionTargetDirectory;
        // 判断解压文件是在源码项目中 还是在 临时解压目录中
        if (sourceCodeParentPath.startsWith(rootPath)) {
            // 截取根目录， 保留项目文件相对路径
            String sourceCodeRelativePath = sourceCodeParentPath.substring(rootPath.length());
            // 获取解压路径
            decompressionTargetDirectory = decompressionOutputPath + File.separator + sourceCodeRelativePath;
        } else {
            decompressionTargetDirectory = sourceCodeParentPath;
        }

        String fileName = file.getName();
        String fileNameWithoutSuffix = FileUtil.mainName(file);
        if (StringUtils.isBlank(fileNameWithoutSuffix)) {
            fileNameWithoutSuffix = fileName.startsWith(".") ? fileName.substring(1) : fileName;
        }
        // 拼接临时目录下解压文件路径
        String targetDirectory = decompressionTargetDirectory + File.separator + fileNameWithoutSuffix;

        // 判断源码文件下 是否有压缩包 解压之后的相同文件名, 防止 后续压缩文件的时候出现错误
        String sourceCodeDecompressionDirectory = sourceCodeParentPath + File.separator + fileNameWithoutSuffix;

        // 判断文件夹是否存在
        if(FileUtil.file(targetDirectory).exists() || FileUtil.file(sourceCodeDecompressionDirectory).exists()) {
            targetDirectory = targetDirectory + "_" + DateUtil.format(new Date(), DATE_FORMAT);
        }
        return targetDirectory;
    }


    /**
     * 解压文件目录下压缩包
     * @param scanRootPath 扫描源目录
     * @param filePathCollect 文件信息存储对象
     * @param excludePaths  排除目录
     * @param compressedFiles  压缩文件集合
     * @param decompressionOutputPath  解压文件输出目录
     * @param executorPool 解压线程池
     * @param allFileList  存储要生成指纹的文件
     */
    public static void decompressSubFiles(String scanRootPath, FilePathCollect filePathCollect, List<String> excludePaths, List<String> compressedFiles, String decompressionOutputPath, Executor executorPool, List<String> allFileList) {

        List<String> allCompressedSubFiles = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(compressedFiles.size());
        compressedFiles.forEach(compressedFile -> executorPool.execute(() -> {
            try {
                File file = new File(compressedFile);
                String archiverName = FileUtil.getType(file);
                // 校验文件是否是支持得压缩文件
                if(ARCHIVER_NAME_LIST.contains(archiverName)) {
                    String targetDirectory = getTargetDirectory(file, scanRootPath, decompressionOutputPath);
                    File targetFile = FileUtil.file(targetDirectory);
                    if (!targetFile.exists()) {
                        Files.createDirectories(targetFile.toPath());
                    }
                    if (isAdd(targetFile.getAbsolutePath())) {
                        rootCompressPathMap.put(file.getAbsolutePath(), targetDirectory);
                    }
                    // 判断是否解压成功
                    if(unCompressMain(file, targetFile, archiverName, scanRootPath) && targetFile.exists()) {
                        List<String> compressedSubFiles = new ArrayList<>();
                        List<String> fileList = WalkFileTreeUtils.walkFileTreeList(targetFile.getAbsolutePath(), filePathCollect, excludePaths, true, 1, compressedSubFiles);
                        if (CollectionUtils.isNotEmpty(fileList)) {
                            allFileList.addAll(fileList);
                        }
                        if (CollectionUtils.isNotEmpty(compressedSubFiles)) {
                            allCompressedSubFiles.addAll(compressedSubFiles);
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("decompression file exception!", e);
            } finally {
                latch.countDown();
            }
        }));
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error("awaiting decompression completion failed", e);
        }
        if (CollectionUtils.isNotEmpty(allCompressedSubFiles)) {
            decompressSubFiles(scanRootPath, filePathCollect, excludePaths, allCompressedSubFiles, decompressionOutputPath, executorPool, allFileList);
        }
    }


        /**
         * 查找压缩文件 并递归解压缩
         * @param sourcePath 源码路径
         * @param decompressionOutputPath 解压文件输出路径
         */
    private static void findAndUnCompress(String sourcePath, String decompressionOutputPath){
        findAndUnCompress(sourcePath, decompressionOutputPath, null);
    }

    /**
     * 解压入口
     * @param compressFile
     * @param targetFile
     * @param archiverName
     * @return
     */
    public static boolean unCompressMain(File compressFile, File targetFile, String archiverName, String sourcePath){


        logger.debug("Start decompressing: " + compressFile.getAbsolutePath());
        String targetPath = "";
        targetPath = targetFile.getAbsolutePath();
        if (StringUtils.isNotBlank(archiverName) && archiverName.contains("tar.gz")){
            try {
                MagicMatch match = Magic.getMagicMatch(compressFile,false);
                if (ObjectUtil.isNotNull(match) && StringUtils.isNotBlank(match.getExtension()) && match.getExtension().equals("tar")){
                    archiverName = match.getExtension();
                    String sourceParentPath = compressFile.getParentFile().getAbsolutePath();
                    targetPath = sourceParentPath + File.separator + FileUtil.mainName(targetFile.getAbsolutePath());
                    if(FileUtil.file(targetPath).exists()){
                        targetPath = targetPath + "_" + DateUtil.format(new Date(), DATE_FORMAT);
                    }
                }
            } catch (Exception e) {
                logger.warn("decompress tar.gz file failed:", e);
            }
        }
        File rootFile = new File(sourcePath);
        FileDecompressor fileDecompressor = FileDecompressorFactory.createFileDecompressor(archiverName);
        //记录映射关系
        softWarePackagePathMap.put(rootFile.getName()  + (compressFile.getAbsolutePath().substring(sourcePath.length()).replaceAll("\\\\", "/"))
                ,targetPath.replaceAll("\\\\", "/"));
        if (fileDecompressor != null) {
            String decompressPath = fileDecompressor.decompress(compressFile.getAbsolutePath(), targetPath, archiverName, null);
            if(StringUtils.isNotBlank(decompressPath) && !targetFile.getAbsolutePath().equals(decompressPath)){
                softWarePackagePathMap.put(rootFile.getName()  +(targetFile.getAbsolutePath().substring(sourcePath.length()).replaceAll("\\\\", "/"))
                        ,rootFile.getName() + (decompressPath.substring(sourcePath.length()).replaceAll("\\\\", "/")));
            }

            // 如果已经存在该Key 则替换value值
            if (isAdd(decompressPath) || rootCompressPathMap.containsKey(compressFile.getAbsolutePath())) {
                rootCompressPathMap.put(compressFile.getAbsolutePath(), decompressPath);
            }
            return decompressPath != null;
        }
        return false;
    }


    public static String unCompressFile(File compressFile, String archiverName){
        if (compressFile == null || !compressFile.exists()) {
            return null;
        }
        String tarPath = compressFile.getParentFile().getAbsolutePath();
        String target = tarPath + File.separator + FileUtil.mainName(compressFile);
        // 判断文件夹是否存在
        if(FileUtil.file(target).exists()){
            String currentDate = DateUtil.format(new Date(), DATE_FORMAT);
            target = target + "_" + currentDate;
        }
        File targetFile = FileUtil.file(target);

        logger.debug("Start decompressing: " + compressFile.getAbsolutePath());
        FileDecompressor fileDecompressor = FileDecompressorFactory.createFileDecompressor(archiverName);
        if (fileDecompressor != null) {
            return fileDecompressor.decompress(compressFile.getAbsolutePath(), targetFile.getAbsolutePath(), archiverName, null);
        }
        return null;
    }


    public static void isDirectoryAndDel(String decompressedDirPath, File decompressedDirFile) {
        try {
            if (decompressedDirFile.exists() && decompressedDirFile.isDirectory()) {
                FileUtil.del(decompressedDirFile);
            }
        } catch (Exception e) {
            logger.warn("Decompression path processing exception! {}", decompressedDirPath, e);
        }
    }


    public static String generateDecompressedDirPath(String compressedFileFullPath, String decompressedDirPath, Set<String> acceptedFileSuffixes) {
        if (StringUtils.isBlank(decompressedDirPath)) {
            String fileNameWithSuffix = extractFileName(compressedFileFullPath);
            String suffix = acceptedFileSuffixes.stream().filter(s -> fileNameWithSuffix.endsWith(s)).findFirst().orElse(null);
            decompressedDirPath = compressedFileFullPath.substring(0, compressedFileFullPath.lastIndexOf(suffix)) + File.separator;
        }

        if (!decompressedDirPath.endsWith(File.separator)) {
            decompressedDirPath += File.separator;
        }
        return decompressedDirPath;
    }


    public static String extractFileNameWithoutSuffix(String fullFilePath, Set<String> acceptedFileSuffixes) {
        String fileNameWithSuffix = extractFileName(fullFilePath);
        String suffix = acceptedFileSuffixes.stream().filter(s -> fileNameWithSuffix.endsWith(s)).findFirst().orElse(null);
        if (suffix == null) {
            return fileNameWithSuffix;
        } else {
            return fileNameWithSuffix.substring(0, fileNameWithSuffix.lastIndexOf(suffix));
        }
    }



    public static final String extractFileName(String fullFilePath) {
        int lastSeparatorIndex = fullFilePath.lastIndexOf('/');
        lastSeparatorIndex = lastSeparatorIndex == -1 ? fullFilePath.lastIndexOf("\\") : lastSeparatorIndex;
        return fullFilePath.substring(lastSeparatorIndex + 1);
    }



    /**
     * 判断是否新增到 targetFileSet
     * @param targetPath
     * @return
     */
    public static boolean isAdd(String targetPath){
        if(StringUtils.isBlank(targetPath)){
            return false;
        }

        for (Map.Entry<String, String> entry : rootCompressPathMap.entrySet()) {
            if(targetPath.startsWith(entry.getValue() + File.separator)){
                return false;
            }
        }
        return true;
    }


    /**
     * 清除目标解压文件
     */
    public static void removeTargetFile() {
        if(CollectionUtil.isNotEmpty(rootCompressPathMap)){
            System.gc();
            try {
                DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
                FileUtil.del(directoryManager.getDecompressionDirectory().getAbsolutePath());
            } catch (IORuntimeException e) {
                logger.error("Failed to delete the extracted file，" + e.getMessage());
            }
        }
    }


/*    public static void main(String[] args) {

        String compressedFileFullPath = "C:\\Users\\Administrator\\Desktop\\projectTest\\unzip\\.tar.bz2\\busybox-1.34.0";
        CompressExtractor.unCompressAll(compressedFileFullPath);
        System.out.println("123");
//        System.out.println(FileUtil.getType(new File(compressedFileFullPath)));

    }*/


}
