package com.sectrend.buildscan.finder;


import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public interface FileFinder {

    @Nullable
    default File findFile(File directory, String filenamePattern, boolean followSymLinks, int depth) {
        List<File> foundFiles = findFiles(directory, Collections.singletonList(filenamePattern), followSymLinks, depth);
        return foundFiles.isEmpty() ? null : foundFiles.get(0);
    }

    @NotNull
    default List<File> findFiles(File directory, List<String> filenamePatterns, boolean followSymLinks, int depth) {
        WildcardFileFilter filter = new WildcardFileFilter(filenamePatterns);
        Predicate<File> wildcardFilter = filter::accept;
        return findFiles(directory, wildcardFilter, followSymLinks, depth, true);
    }

    @NotNull List<File> findFiles(File directory, Predicate<File> filter, boolean followSymLinks, int depth, boolean isMatchingDirectories);

    @NotNull
    default List<File> findFiles(File directory, Predicate<File> filter, boolean followSymLinks, int depth) {
        return findFiles(directory, filter, followSymLinks, depth, true);
    }


    default File findFile(File directory, String filenamePattern) {
        return findFile(directory, filenamePattern, 0);
    }

    default File findFile(File directory, String filenamePattern, int depth) {
        List<File> foundFiles = findFiles(directory, Collections.singletonList(filenamePattern), depth);
        if (CollectionUtils.isNotEmpty(foundFiles)) return foundFiles.get(0);
        return null;
    }

    default List<File> findFiles(File directory, String filenamePattern) {
        return findFiles(directory, Collections.singletonList(filenamePattern), 0);
    }

    default List<File> findFiles(File directory, String filenamePattern, int depth) {
        return findFiles(directory, Collections.singletonList(filenamePattern), depth);
    }

    default List<File> findFiles(File directory, List<String> filenamePatterns) {
        return findFiles(directory, filenamePatterns, 0);
    }

    default List<File> findFiles(File directory, List<String> filenamePatterns, int depth) {
        return findFiles(directory, filenamePatterns, depth, true);
    }

    List<File> findFiles(File paramFile, List<String> paramList, int paramInt, boolean paramBoolean);

    public List<String> getFilePaths();

    public void setFilePaths(List<String> filePaths);

}
