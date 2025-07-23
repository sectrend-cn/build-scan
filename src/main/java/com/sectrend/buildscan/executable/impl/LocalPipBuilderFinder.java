package com.sectrend.buildscan.executable.impl;

import com.sectrend.buildscan.buildTools.ScannableException;
import com.sectrend.buildscan.executable.finder.PipBuilderFinder;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LocalPipBuilderFinder implements PipBuilderFinder {

    private final DirectoryManager directoryManager;

    private File foundBuilder = null;

    private boolean hasFoundBuilder = false;

    public LocalPipBuilderFinder(DirectoryManager directoryManager) {
        this.directoryManager = directoryManager;
    }

    public File findPipBuilder() throws ScannableException {
        try {
            if (!this.hasFoundBuilder) {
                this.hasFoundBuilder = true;
                this.foundBuilder = installBuilder();
            }
            return this.foundBuilder;
        } catch (Exception e) {
            throw new ScannableException(e);
        }
    }

    private File installBuilder() throws IOException {
        try (InputStream builderFileStream = getClass().getResourceAsStream("/pip-build.py")) {
            if (builderFileStream == null) {
                throw new IOException("Build script not found.");
            }

            // 读取文件内容
            String scriptContents = IOUtils.toString(builderFileStream, StandardCharsets.UTF_8);

            // 获取目标文件并写入内容
            File builderScript = directoryManager.getSharedFile("pip", "pip-build.py");
            FileUtils.writeStringToFile(builderScript, scriptContents, StandardCharsets.UTF_8);

            return builderScript;
        }
    }
}
