package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

@Slf4j
public class BzFileDecompressor implements FileDecompressor{

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("tar.bz2", "bz2");

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName, Integer bufferSize) {
        if (!decompressedDirPath.endsWith(".tar") && !"bz2".equals(archiverName)) {
            decompressedDirPath += ".tar";
        }
        File decompressedDirFile = new File(decompressedDirPath);
        CompressExtractor.isDirectoryAndDel(decompressedDirPath, decompressedDirFile);
        try (FileInputStream fis = new FileInputStream(compressedFileFullPath);
             FileOutputStream fos = new FileOutputStream(decompressedDirPath);
             BZip2CompressorInputStream bz2In = new BZip2CompressorInputStream(fis)) {

            bufferSize = bufferSize == null ? BUFFER_SIZE : bufferSize;
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = bz2In.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            try {
                fos.close();
            } catch (IOException e) {
                log.error("File output stream closing failed！", e);
            }
            if (!decompressedDirFile.exists()) {
                return null;
            }
            archiverName = FileUtil.getType(decompressedDirFile);
            if (CompressExtractor.ARCHIVER_NAME_LIST.contains(archiverName)) {
                String unCompressPath = CompressExtractor.unCompressFile(decompressedDirFile, archiverName);
                if (StringUtils.isNotBlank(unCompressPath)) {
                    decompressedDirFile.delete();
                    return unCompressPath;
                }
            }
            return decompressedDirPath;
        } catch (Exception e) {
            log.warn("Bx2 decompression exception! compressedFilePath={}", compressedFileFullPath, e);
        }
        return null;
    }


}
