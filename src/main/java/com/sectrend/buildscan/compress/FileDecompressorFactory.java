package com.sectrend.buildscan.compress;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author wuguangya
 * @since 2023/3/28
 */
public class FileDecompressorFactory {

    private static final List<? extends FileDecompressor> fileDecompressors = Lists.newArrayList(
            new ApkFileDecompressor(),
            new BzFileDecompressor(),
            new DefaultFileDecompressor(),
            new GzFileDecompressor(),
            new RpmFileDecompressor(),
            new XzFileDecompressor(),
            new ZipFileDecompressor(),
            new RarFileDecompressor(),
            new SevenZFileDecompressor()
    );

    public static FileDecompressor createFileDecompressor(String archiverName) {
        for (FileDecompressor fileDecompressor : fileDecompressors) {
            if (fileDecompressor.accept(archiverName)) {
                return fileDecompressor;
            }
        }
        return null;
    }
}
