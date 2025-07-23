package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * @author wuguangya
 * @since 2023/3/28
 */
@Slf4j
public class GzFileDecompressor implements FileDecompressor {

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("gz");

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName, Integer bufferSize) {
        if (StringUtils.isBlank(compressedFileFullPath)) {
            log.warn("Compressed file path should not be empty!");
            return null;
        }

        File uncompressedDirFile = new File(decompressedDirPath);
        CompressExtractor.isDirectoryAndDel(decompressedDirPath, uncompressedDirFile);
//        decompressedDirPath = CompressExtractor.generateDecompressedDirPath(compressedFileFullPath, decompressedDirPath, ACCEPTED_FILE_SUFFIXES);
//        String uncompressedFileName = CompressExtractor.extractFileNameWithoutSuffix(compressedFileFullPath, ACCEPTED_FILE_SUFFIXES);
        bufferSize = bufferSize == null ? BUFFER_SIZE : bufferSize;
        try (FileInputStream fis = new FileInputStream(compressedFileFullPath);
             GZIPInputStream gzis = new GZIPInputStream(fis);
             BufferedInputStream bis = new BufferedInputStream(gzis);
             FileOutputStream fos = new FileOutputStream(uncompressedDirFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)){

            byte[] buf = new byte[bufferSize];
            int len;
            while((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
            archiverName = FileUtil.getType(uncompressedDirFile);
            // 判断解压后文件是否为tar 如果为tar则继续解压， 解压完后删除tar
            if (CompressExtractor.ARCHIVER_NAME_LIST.contains(archiverName)) {

                String unCompressPath = CompressExtractor.unCompressFile(uncompressedDirFile, archiverName);
                if (StringUtils.isNotBlank(unCompressPath)) {
                    uncompressedDirFile.delete();
                    return unCompressPath;
                }
            }
            return decompressedDirPath;
        } catch (Exception e) {
            log.warn("Failed to decompress file! compressedFilePath={} ", compressedFileFullPath, e);
        }
        return null;
    }

/*    @Override
    public String getFileNameWithoutSuffix(String compressedFileFullPath) {
        *//*if (NOT_ACCEPTED_FILE_SUFFIXES.stream().anyMatch(suffix -> compressedFileFullPath.endsWith(suffix))) {
            return null;
        }*//*

        String fileNameWithSuffix = CompressExtractor.extractFileName(compressedFileFullPath);
        String suffix = ACCEPTED_FILE_SUFFIXES.stream().filter(s -> fileNameWithSuffix.endsWith(s)).findFirst().orElse(null);
        if (suffix != null) {
            return fileNameWithSuffix.substring(0, fileNameWithSuffix.lastIndexOf(suffix));
        } else {
            return null;
        }
    }*/
}
