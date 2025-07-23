package com.sectrend.buildscan.finder;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author huishun.yi
 * @Date 2024/2/27 17:58
 */
public class FinderReferenceFile {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileFinder fileFinder;

    private List<File> referenceFileList = Lists.newArrayList();


    public FinderReferenceFile(FileFinder fileFinder) {
        this.fileFinder = fileFinder;
    }

    public List<File> getReferenceFileList() {
        return referenceFileList;
    }


    public void findReferenceFiles(File scanDirectory) {

        if (scanDirectory == null || !scanDirectory.exists() || Files.isSymbolicLink(scanDirectory.toPath())) {
            return;
        }
        try {
            //查找目录下全部文件
            Stream<Path> pathStream = Files.list(scanDirectory.toPath());
            Set<File> fileDirList = null;
            if (pathStream != null) {
                List<File> fileList = pathStream.<File>map(Path::toFile).collect(Collectors.toList());
                fileDirList = fileList.stream()
                        .filter(File::isDirectory)
                        .collect(Collectors.toSet());
            }

            List<File> fileList = fileFinder.findFiles(scanDirectory, "*.gradle");
            if (CollectionUtils.isNotEmpty(fileList)) {
                referenceFileList.addAll(fileList);
            }
            if (CollectionUtils.isNotEmpty(fileDirList)) {
                for (File file : fileDirList) {
                    findReferenceFiles(file);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in finding reference files！", e);
        }
    }


}
