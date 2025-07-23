package com.sectrend.buildscan.compress;

import cn.hutool.core.io.FileUtil;
import com.sectrend.buildscan.executable.ExecutionOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;

import java.io.File;
import java.util.Set;

/**
 * @Author huishun.yi
 * @Date 2023/9/22 20:22
 */
@Slf4j
public class RpmFileDecompressor implements FileDecompressor{

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("rpm");

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName,  Integer bufferSize) {
        try {
//            String command = "rpm2cpio " + compressedFileFullPath + " | cpio -id --directory=" + decompressedDirPath;
            String decompressedCPIOPath = decompressedDirPath + ".cpio";
            String command = "rpm2cpio " + compressedFileFullPath + " > " + decompressedCPIOPath;
            String[] commands = {"bash", "-c", command};
            ExecutionOutput execute = CompressExtractor.simpleExecutableRunner.execute(commands);
            if (execute.getExitCode() != 0) {
                log.warn("rpm Decompression failure: " + execute.getExceptionOutput());
                return null;
            }
            File decompressedCPIOFile = new File(decompressedCPIOPath);
            if (decompressedCPIOFile.exists()) {
                String compressFile = CompressExtractor.unCompressFile(decompressedCPIOFile, "cpio");
                FileUtil.del(decompressedCPIOFile);
                return compressFile;
            }
        } catch (Exception e) {
            log.warn("rpm Decompression failure! compressedFilePath={}", compressedFileFullPath, e);
        }
        return null;
    }

 /*   @Override
    public String getFileNameWithoutSuffix(String compressedFileFullPath) {
        return null;
    }*/
}
