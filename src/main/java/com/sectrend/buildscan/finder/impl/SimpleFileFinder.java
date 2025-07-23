
package com.sectrend.buildscan.finder.impl;

import com.sectrend.buildscan.finder.FileFinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class SimpleFileFinder implements FileFinder {

    List<String> filePaths;

    private List<File> findFiles(File directoryToSearch, FilenameFilter filenameFilter, int depth, boolean findInsideMatchingDirectories) {
        List<File> foundFiles = new ArrayList<>();
        if (depth < 0 || Files.isSymbolicLink(directoryToSearch.toPath())) {
            return foundFiles;
        }
        boolean filterByName = true;
        //外面有单例模式使用的
        List<String> allPaths = null;
        if (CollectionUtils.isEmpty(filePaths)) {
            filterByName = false;
            File[] files = directoryToSearch.listFiles();
            if (files == null) {
                log.warn("no sub files found {}", directoryToSearch.getAbsolutePath());
                return foundFiles;
            }
            allPaths = Arrays.stream(Objects.requireNonNull(directoryToSearch.listFiles())).map(File::getAbsolutePath).collect(Collectors.toList());
        } else {
            allPaths = filePaths;
        }
        if (CollectionUtils.isEmpty(allPaths))
            return foundFiles;
        for (String path : allPaths) {
            //现在是路径,获取文件名
            String[] split = path.split("[/\\\\]");
            String name = split[split.length -1];
            boolean matches = filenameFilter.accept(directoryToSearch, name);
            if (matches) {
                foundFiles.add(new File(path));
            } else if (!filterByName && findInsideMatchingDirectories) {
                File file = new File(path);
                if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
                    foundFiles.addAll(findFiles(file, filenameFilter, depth - 1, findInsideMatchingDirectories));
                }
            }
        }
        return foundFiles;
    }


    @Override
    public List<String> getFilePaths() {
        return filePaths;
    }

    @Override
    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }

    @NotNull
    @Override
    public List<File> findFiles(File directoryToSearch, Predicate<File> filter, boolean followSymLinks, int depth, boolean findInsideMatchingDirectories) {
        List<File> foundFiles = new ArrayList<>();
        if (depth < 0) {
            return foundFiles;
        }
        if (!shouldFindInDirectory(directoryToSearch, followSymLinks)) {
            return foundFiles;
        }
        File[] allFiles = directoryToSearch.listFiles();
        if (allFiles == null) {
            return foundFiles;
        }
        for (File file : allFiles) {
            boolean matches = filter.test(file);
            if (matches) {
                foundFiles.add(file);
            }
            if ((!matches || findInsideMatchingDirectories) && shouldFindInDirectory(file, followSymLinks)) {
                foundFiles.addAll(findFiles(file, filter, followSymLinks, depth - 1, findInsideMatchingDirectories));
            }
        }

        return foundFiles;
    }


    private boolean shouldFindInDirectory(File file, boolean followSymLinks) {
        return (file.isDirectory() && (!Files.isSymbolicLink(file.toPath()) || followSymLinks)) && linkPointsToValidDirectory(file);
    }

    private boolean linkPointsToValidDirectory(File directory) {
        Path linkTarget;
        try {
            linkTarget = directory.toPath().toRealPath();
        } catch (IOException e) {
            return false;
        }
        return linkTarget.toFile().isDirectory();
    }

    @Override
    public List<File> findFiles(File directoryToSearch, List<String> filenamePatterns, int depth, boolean findInsideMatchingDirectories) {
        return findFiles(directoryToSearch, (FilenameFilter) new WildcardFileFilter(filenamePatterns), depth, findInsideMatchingDirectories);
    }
}
