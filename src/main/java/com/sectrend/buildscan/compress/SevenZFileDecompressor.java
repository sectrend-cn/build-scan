package com.sectrend.buildscan.compress;


import lombok.extern.slf4j.Slf4j;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Set;

@Slf4j
public class SevenZFileDecompressor implements FileDecompressor {
    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("7z");


    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }


    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName, Integer bufferSize) {

        File decompressedDirFile = new File(decompressedDirPath);
        if (StringUtils.isBlank(compressedFileFullPath)) {
            log.warn("Compressed file path should not be empty!");
            return null;
        }

        RandomAccessFile randomAccessFile = null;
        IInArchive inArchive = null;

        try {
            // 打开 .7z 文件
            randomAccessFile = new RandomAccessFile(compressedFileFullPath, "r");
            inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));

            // 获取简单接口
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();

            // 遍历压缩包中的每个文件
            for (ISimpleInArchiveItem item : simpleInArchive.getArchiveItems()) {
                if (!item.isFolder()) {

                    // 构建解压文件的完整路径
                    String itemPath = decompressedDirPath + File.separator + item.getPath();
                    File outFile = new File(itemPath);

                    // 确保父目录存在
                    File parentDir = outFile.getParentFile();
                    if (!parentDir.exists()) {
                        parentDir.mkdirs(); // 创建父目录
                    }
                    // 解压文件
                    try (OutputStream out = new FileOutputStream(outFile)) {
                        ExtractOperationResult result = item.extractSlow(data -> {
                            try {
                                out.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return data.length;
                        });

                        if (result == ExtractOperationResult.OK) {
                            log.info("Extracted: {}", item.getPath());
                        } else {
                            log.error("Error extracting: {}", item.getPath());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("File path:{}, 7z decompression failure!", compressedFileFullPath, e);
            return null;
        } finally {
            // 关闭资源
            try {
                if (inArchive != null) {
                    inArchive.close();
                }
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            } catch (IOException e) {
                log.warn("File path:{}, close 7z decompression failure!", compressedFileFullPath, e);
            }
        }
        return decompressedDirFile.getAbsolutePath();
    }

}