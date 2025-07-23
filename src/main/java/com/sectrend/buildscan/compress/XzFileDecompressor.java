package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Set;

@Slf4j
public class XzFileDecompressor implements FileDecompressor{

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("tar.xz", "xz");

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName,  Integer bufferSize) {
        if (!decompressedDirPath.endsWith(".tar") && !"xz".equals(archiverName)) {
            decompressedDirPath += ".tar";
        }
        File uncompressedDirFile = new File(decompressedDirPath);
        CompressExtractor.isDirectoryAndDel(decompressedDirPath, uncompressedDirFile);
        try (InputStream inputStream = new FileInputStream(compressedFileFullPath);
             XZCompressorInputStream xzInputStream = new XZCompressorInputStream(inputStream);
             OutputStream outputStream = new FileOutputStream(decompressedDirPath)) {

            bufferSize = bufferSize == null ? BUFFER_SIZE : bufferSize;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = xzInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                log.error("Failed to close file output stream!", e);
            }
            File decompressedDirFile = new File(decompressedDirPath);
            /*archiverName = FileUtil.getType(decompressedDirFile);
            if (CompressExtractor.ARCHIVER_NAME_TAR.equals(archiverName)) {
                CompressExtractor.unTar(decompressedDirFile, bufferSize);
            }*/
            archiverName = FileUtil.getType(decompressedDirFile);
            if (CompressExtractor.ARCHIVER_NAME_LIST.contains(archiverName)) {
                String unCompressPath = CompressExtractor.unCompressFile(decompressedDirFile, archiverName);
                if (StringUtils.isNotBlank(unCompressPath)) {
                    decompressedDirFile.delete();
                    return unCompressPath;
                }
            }
            return decompressedDirPath;
        } catch (IOException e) {
            log.warn("Abnormal xz decompression! compressedFilePath={}", compressedFileFullPath, e);
        }
        return null;
    }


/*    @Override
    public String getFileNameWithoutSuffix(String compressedFileFullPath) {
        return null;
    }*/
}
