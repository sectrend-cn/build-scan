package com.sectrend.buildscan.utils;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import com.sectrend.buildscan.buildTools.scanner.BlacklistRules;
import com.sectrend.buildscan.compress.CompressExtractor;
import com.sectrend.buildscan.model.FilePathCollect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.util.Strings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 遍历文件目录生成指纹文件工具类
 */
@Slf4j
public class WalkFileTreeUtils {

    /*public static StringBuilder walkFileTree(String dir,String taskDir)throws Exception{
        StringBuilder wfp = new StringBuilder();
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                File file = dir.toFile();
                String name = file.getName();
                if (BlacklistRules.filteredDirs(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if (BlacklistRules.filteredDirExt(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!Files.isDirectory(file) && !BlacklistRules.hasBlacklistedExt(file.toString()) && !BlacklistRules.filteredFiles(file.getFileName().toString())) {
                    try {
                        File projectDir = new File(taskDir);
                        String projectName = projectDir.getName();
                        String projectPath = projectDir.getCanonicalPath();
                        String wfpString = Winnowing.wfpForFile(projectName + file.toString().replace(projectPath, ""), file.toString(),);
                        if (wfpString != null && !wfpString.isEmpty())
                            wfp.append(wfpString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return wfp;
    }*/


    public static List<String> walkFileTreeList(String dir, FilePathCollect filePathCollect, List<String> excludePaths, boolean isCompress, int isUnZip, List<String> compressedFiles) throws Exception {
        List<String> fileList = new ArrayList<>();

        final Set<String> LICENSE_FILE_NAME_KEYWORDS = Sets.newHashSet("license", "licenses", "licence", "licences");
        dir = !dir.endsWith(File.separator) ? dir + File.separator : dir;
        String[] split = dir.split("[/\\\\]");
        String floderName = split[split.length - 1];
        String finalDir = dir;
        Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {

                File file = dir.toFile();
                String name = file.getName();
                if (BlacklistRules.filteredDirs(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                try {
                    if (CollectionUtils.isNotEmpty(excludePaths)) {
                        String relativePath = (isCompress ? Strings.EMPTY : (floderName + File.separator) + (dir.toAbsolutePath().toString() + File.separator).substring(finalDir.length())).replace("\\", "/");
                        if (excludePaths.stream().anyMatch(s -> Pattern.matches(s, relativePath))) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                } catch (Throwable t) {
                    log.error("failed to filter file by {}", JSON.toJSONString(excludePaths), t);
                }
                filePathCollect.getDirs().add(file.toString());
               /* if (BlacklistRules.filteredDirExt(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }*/
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isNotBlacklistFile(file)) {
                    try {
                        if (CollectionUtils.isNotEmpty(excludePaths)) {
                            String relativePath = (isCompress ? Strings.EMPTY : (floderName + File.separator) + file.toAbsolutePath().toString().substring(finalDir.length())).replace("\\", "/");
                            if (excludePaths.stream().anyMatch(s -> Pattern.matches(s, relativePath))) {
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                        }
                    } catch (Throwable t) {
                        log.error("failed to filter file by {}", JSON.toJSONString(excludePaths), t);
                    }
                    fileList.add(file.toString());

                }
                if (attrs.isSymbolicLink()) {
                    filePathCollect.getSymbolicLinks().add(file.toString());
                } else {
                    filePathCollect.getFiles().add(file.toString());
                    String relativePath = file.toString().replace(finalDir, "");
                    String[] pathSplits = relativePath.split("[/\\\\]");
                    if (pathSplits.length <= 2 && LICENSE_FILE_NAME_KEYWORDS.contains(pathSplits[pathSplits.length - 1].toLowerCase())) {
                        filePathCollect.getProjectLicenseFile().add(file.toString());
                    }
                }

                try {
                    if (isUnZip == 1) {
                        String archiverName = FileUtil.getType(file.toFile());
                        // 校验文件是否是支持得压缩文件
                        if(CompressExtractor.ARCHIVER_NAME_LIST.contains(archiverName)) {
                            compressedFiles.add(file.toString());
                        }
                    }
                } catch (Throwable e) {
                    log.error("Failed to retrieve file information!", e);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                log.error("File cannot be accessed, file path: {}", file.toString(), exc);
                return FileVisitResult.CONTINUE;
            }

        });
        return fileList;
    }


    /**
     * 是否是黑名单文件
     * @return
     */
    public static boolean isNotBlacklistFile(Path file){
        return !(Files.isDirectory(file) || file.toFile().length() == 0 || !isRegularFile(file.toString()) || isStMode33024(file)) &&
                (BlacklistRules.filteredWhiteFile(file.getFileName().toString()) || !BlacklistRules.hasBlacklistedExt(file.getFileName().toString()));
    }



    public static boolean isRegularFile(String filePath) {
        Path path = Paths.get(filePath);
        try {
            return Files.isRegularFile(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private static boolean isStMode33024(Path filepath) {
        try {
            PosixFileAttributes attrs = Files.readAttributes(filepath, PosixFileAttributes.class);
            int stMode = attrs.permissions().toString().hashCode();

            return stMode == 33024;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isBinary(File file)
    {
        boolean isBinary = false;
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
            long len = file.length();
            for (int j = 0; j < (int) len; j++) {
                int t = fin.read();
                if (t < 32 && t != 9 && t != 10 && t != 13) {
                    isBinary = true;
                    break;
                }
            }
        } catch (Exception e) {
           log.error("Determine binary file anomalies ,filepath：{} {}", file.getAbsolutePath(), e.getMessage());
        } finally {
            if(fin != null) {
                try {
                    fin.close();
                } catch (IOException e) {
                    log.error("Close file flow exception ,filepath：{} {}", file.getAbsolutePath(), e.getMessage());
                }
            }
        }
        return isBinary;
    }

}
