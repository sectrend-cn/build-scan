package com.sectrend.buildscan.executable.impl;

import com.sectrend.buildscan.finder.FileFinder;
import com.sectrend.buildscan.system.OSType;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


public class SimpleExecutableFinder {
    private final List<String> extensions;

    private final FileFinder fileFinder;

    public static SimpleExecutableFinder forCurrentOperatingSystem(FileFinder fileFinder) {
        return forOperatingSystem(OSType.determineFromSystem(), fileFinder);
    }

    public static SimpleExecutableFinder forOperatingSystem(OSType osType, FileFinder fileFinder) {
        if (osType == OSType.WINDOWS)
            return new SimpleExecutableFinder(Arrays.asList(new String[]{".cmd", ".bat", ".exe"}), fileFinder);
        return new SimpleExecutableFinder(Collections.emptyList(), fileFinder);
    }

    public SimpleExecutableFinder(List<String> extensions, FileFinder fileFinder) {
        this.extensions = extensions;
        this.fileFinder = fileFinder;
    }

    private List<String> executablesFromName(String name) {
        if (this.extensions.isEmpty())
            return Collections.singletonList(name);
        return (List<String>) this.extensions.stream().map(ext -> name + ext).collect(Collectors.toList());
    }

    public File findExecutable(String exe, File path) {
        return findExecutable(exe, Collections.singletonList(path));
    }

    public File findExecutable(String exe, List<File> paths) {
        List<String> executables = executablesFromName(exe);

        // 遍历所有路径，查找每个可能的可执行文件
        return paths.stream()
                .flatMap(path -> executables.stream()
                        .map(executable -> this.fileFinder.findFile(path, executable))
                        .filter(file -> file != null && file.exists() && file.canExecute()))
                .findFirst()
                .orElse(null);
    }
}
