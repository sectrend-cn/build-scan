package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ZipUtil;
import com.sectrend.buildscan.utils.StringEncodingDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Set;

/**
 * @author wuguangya
 * @since 2023/3/28
 */
@Slf4j
public class ZipFileDecompressor implements FileDecompressor {

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("jar", "war", "zip");


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

//        decompressedDirPath = CompressExtractor.generateDecompressedDirPath(compressedFileFullPath, decompressedDirPath, ACCEPTED_FILE_SUFFIXES);
        File decompressedDirFile = new File(decompressedDirPath);
        BufferedInputStream fileInputStream;
        ArchiveInputStream zipInputStream = null;
        bufferSize = bufferSize == null ? BUFFER_SIZE : bufferSize;
        try {
            if (!decompressedDirFile.exists()) {
                decompressedDirFile.mkdirs();
            }
            File compressedFile = new File(compressedFileFullPath);
            if ("war".equals(archiverName)) {
                ZipUtil.unzip(compressedFile, decompressedDirFile);
                return decompressedDirFile.getAbsolutePath();
            }
            fileInputStream = FileUtil.getInputStream(compressedFile);
            ArchiveStreamFactory streamFactory = new ArchiveStreamFactory("UTF-8");
//            String suffix = FileNameUtil.getSuffix(compressedFileFullPath);
//            String archiverName = "war".equals(suffix) ? "zip" : suffix;
            zipInputStream = streamFactory.createArchiveInputStream(archiverName, fileInputStream);
            if (zipInputStream instanceof ZipArchiveInputStream) {
                zipInputStream = new ZipArchiveInputStream(fileInputStream, "UTF8", true, true);
            }
            ArchiveEntry entry;
            File outItemFile;
            String entryName;
            while (true) {
                entry = zipInputStream.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (!zipInputStream.canReadEntryData(entry)) {
                    log.warn("decompressing entry failed, entry={}", entry.getName());
                    break;
                }

                byte[] rawName = ((ZipArchiveEntry) entry).getRawName();
                if (rawName != null && rawName.length > 0) {
                    String charset = StringEncodingDetector.detectCharset(rawName);
                    entryName = new String(rawName, charset);
                } else {
                    entryName = entry.getName();
                }
                //log.debug("decompressing entry:{}", entryName);
                outItemFile = FileUtil.file(decompressedDirFile, entryName);
                if (entry.isDirectory()) {
                    outItemFile.mkdirs();
                } else {
                    if (outItemFile.isDirectory()) {
                        continue;
                    }
                    writeFromStream(zipInputStream, outItemFile, bufferSize);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to decompress file! compressedFilePath={}", compressedFileFullPath, e);
            return decompressedDirFile.getAbsolutePath();
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                }
            }
        }
        return decompressedDirFile.getAbsolutePath();
    }

/*    @Override
    public String getFileNameWithoutSuffix(String compressedFileFullPath) {
        String fileNameWithSuffix = CompressExtractor.extractFileName(compressedFileFullPath);
        String suffix = ACCEPTED_FILE_SUFFIXES.stream().filter(s -> fileNameWithSuffix.endsWith(s)).findFirst().orElse(null);
        if (suffix != null) {
            return fileNameWithSuffix.substring(0, fileNameWithSuffix.lastIndexOf(suffix));
        } else {
            return null;
        }
    }*/

    private void writeFromStream(InputStream in, File file, int bufferSize) throws IORuntimeException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(FileUtil.touch(file));
            IoUtil.copy(in, out, bufferSize);
        } catch (IOException e) {
            log.warn("Writing content into file failed, file={}", file.getAbsolutePath(), e);
        } finally {
            IoUtil.close(out);
        }
    }
}
