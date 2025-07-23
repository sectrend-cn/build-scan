package com.sectrend.buildscan.compress;

/**
 * @author wuguangya
 * @since 2023/3/28
 */
public interface FileDecompressor {

    Integer BUFFER_SIZE = 8 * 1024;

    boolean accept(String archiverName);

    String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName, Integer bufferSize);

//    String getFileNameWithoutSuffix(String compressedFileFullPath);
}
