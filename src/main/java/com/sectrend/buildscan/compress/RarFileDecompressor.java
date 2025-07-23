package com.sectrend.buildscan.compress;

import com.sectrend.buildscan.executable.ExecutionOutput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;

import java.io.File;
import java.util.Set;

@Slf4j
public class RarFileDecompressor implements FileDecompressor {
    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("rar");

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName,  Integer bufferSize) {
        File decompressedDirFile = new File(decompressedDirPath);
        try {
            //  command :    unrar x -o+ archive.rar /path/to/destination
            String[] commands = {"unrar", "x", "-o+", compressedFileFullPath, decompressedDirPath};
            ExecutionOutput executeRes = CompressExtractor.simpleExecutableRunner.execute(commands);
            if (executeRes.getExitCode() != 0) {
                log.warn("file path:{}, rar decompression failure: {}", compressedFileFullPath, executeRes.getExceptionOutput());
//                return decompressedDirFile.getAbsolutePath();
                return null;
            }
        } catch (Exception e) {
            log.warn("file path:{}, rar decompression failure!", compressedFileFullPath, e);
            return null;
        }
//        return null;
        return decompressedDirFile.getAbsolutePath();
    }
}
