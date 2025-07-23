package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.compress.CompressUtil;
import cn.hutool.extra.compress.extractor.Extractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Set;

/**
 * Default file decompressor, some compressed file types, such as .tar, .tar.gz, could be processed.
 *
 * @author wuguangya
 * @since 2023/3/28
 */
@Slf4j
public class DefaultFileDecompressor implements FileDecompressor {

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("tar", "tar.gz", "cpio", "tgz");

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

//        String archiverName = ACCEPTED_FILE_SUFFIXES.stream().filter(s -> compressedFileFullPath.endsWith(s)).findFirst().orElse(null);
        if (archiverName == null) {
            log.warn("Compressed file type is not supported, file={}", compressedFileFullPath);
            return null;
        }
//        decompressedDirPath = CompressExtractor.generateDecompressedDirPath(compressedFileFullPath, decompressedDirPath, ACCEPTED_FILE_SUFFIXES);
        ArchiveInputStream in = null;
        File uncompressedDirFile = new File(decompressedDirPath);
        try {

            if (!uncompressedDirFile.exists()) {
                uncompressedDirFile.mkdirs();
            }
            if ("cpio".equals(archiverName)) {
                Extractor extractor = CompressUtil.createExtractor(
                        Charset.forName("UTF-8"),
                        archiverName,
                        new File(compressedFileFullPath));
                extractor.extract(uncompressedDirFile);
                return uncompressedDirFile.getAbsolutePath();
            }

            bufferSize = bufferSize == null ? BUFFER_SIZE : bufferSize;

            in = getArchiveInputStream(CharsetUtil.defaultCharsetName(), archiverName, new FileInputStream(compressedFileFullPath));
            Assert.isTrue(null != uncompressedDirFile && ((false == uncompressedDirFile.exists()) || uncompressedDirFile.isDirectory()), "target must be dir.");
            ArchiveEntry entry;
            File outFile;
            while (true) {
                entry = in.getNextEntry();
                if (entry == null) {
                    break;
                }
                if (!in.canReadEntryData(entry)) {
                    // 无法读取的文件直接跳过
                    continue;
                }
                outFile = FileUtil.file(uncompressedDirFile, entry.getName());
                if (entry.isDirectory()) {
                    // 创建对应目录
                    outFile.mkdirs();
                } else {
                    if (outFile.isDirectory()) {
                        continue;
                    }
                    writeFromStream(in, outFile, bufferSize);
                }
            }
            return decompressedDirPath;
        } catch (Exception e) {
            log.warn("Failed to decompress file! compressedFilePath={}.", compressedFileFullPath, e);
        } finally {
            IoUtil.close(in);
        }
        return uncompressedDirFile.getAbsolutePath();
    }


//    @Override
//    public String getFileNameWithoutSuffix(String compressedFileFullPath) {
//        String fileNameWithSuffix = CompressExtractor.extractFileName(compressedFileFullPath);
//        String suffix = ACCEPTED_FILE_SUFFIXES.stream().filter(s -> fileNameWithSuffix.endsWith(s)).findFirst().orElse(null);
//        if (suffix != null) {
//            return fileNameWithSuffix.substring(0, fileNameWithSuffix.lastIndexOf(suffix));
//        } else {
//            return null;
//        }
//    }


    private ArchiveInputStream getArchiveInputStream(String charsetName, String archiverName, InputStream in) throws ArchiveException, IORuntimeException, IOException {
        ArchiveStreamFactory factory = new ArchiveStreamFactory(charsetName);
        in = IoUtil.toBuffered(in);
        if (StrUtil.isBlank(archiverName)) {
            return factory.createArchiveInputStream(in);
        } else if("tgz".equalsIgnoreCase(archiverName) || "tar.gz".equalsIgnoreCase(archiverName)){
            return new TarArchiveInputStream(new GzipCompressorInputStream(in));
        } else {
            return factory.createArchiveInputStream(archiverName, in);
        }
    }


    public File writeFromStream(InputStream in, File file, Integer bufferSize) throws IORuntimeException {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(FileUtil.touch(file));
            IoUtil.copy(in, out, bufferSize);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        } finally {
            IoUtil.close(out);
        }
        return file;
    }

}
