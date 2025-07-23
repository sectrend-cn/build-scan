package com.sectrend.buildscan.compress;

import cn.hutool.core.io.IoUtil;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Sets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * @Author huishun.yi
 * @Date 2023/5/6 15:15
 */
@Slf4j
public class ApkFileDecompressor implements FileDecompressor{

    private static final Set<String> ACCEPTED_FILE_SUFFIXES = Sets.newHashSet("apk");

    private static final String APK_TOOL_JAR = "apktool.jar";

    @Override
    public boolean accept(String archiverName) {
        return ACCEPTED_FILE_SUFFIXES.stream().anyMatch(s -> s.equals(archiverName));
    }

    @Override
    public String decompress(String compressedFileFullPath, String decompressedDirPath, String archiverName, Integer bufferSize) {

        DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();

        File apkToolJarFile = copyApkJarPath(directoryManager.getGetToolPath());

        String [] commands = {"java", "-jar", apkToolJarFile.getAbsolutePath(), "d", compressedFileFullPath, "-o", decompressedDirPath };
        try {
            File decompressedDirFile = new File(decompressedDirPath);
            if (decompressedDirFile.exists()) {
                decompressedDirFile.delete();
            }
            ExecutionOutput execute = CompressExtractor.simpleExecutableRunner.execute(commands);
            if(execute.getExitCode() != 0) {
                log.warn("apk Decompression failure: " + execute.getExceptionOutput());
            }
            return decompressedDirPath;
        } catch (Exception e) {
            log.warn("APK decompression exception! compressedFilePath={}", compressedFileFullPath, e);
        }
        return null;
    }



    public File copyApkJarPath(File toolPath) {
        File apkToolJarFile = new File(toolPath.getAbsolutePath() + File.separator + APK_TOOL_JAR);

        if (apkToolJarFile.exists()) {
            return apkToolJarFile;
        }
        InputStream is = getClass().getResourceAsStream(String.format("/%s", new Object[]{APK_TOOL_JAR}));
        FileOutputStream fos = null;
        int readBytes;
        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            apkToolJarFile.createNewFile();
            fos = new FileOutputStream(apkToolJarFile);
            while ((readBytes = is.read(buffer)) > 0) {
                fos.write(buffer, 0, readBytes);
            }
            return apkToolJarFile;
        } catch (Exception e) {
            log.warn("Copying apkTool.jar exception!", e);
        } finally {
            // close streams
            IoUtil.close(is);
            IoUtil.close(fos);
        }
        return null;
    }


}