package com.sectrend.buildscan.executable.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimpleSystemExecutableFinder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SimpleExecutableFinder executableFinder;

    public SimpleSystemExecutableFinder(SimpleExecutableFinder executableFinder) {
        this.executableFinder = executableFinder;
    }

    public File findExecutable(String executable) {
        String systemPath = System.getenv("PATH");
        List<File> systemPathLocations = (List<File>) Arrays.<String>stream(systemPath.split(File.pathSeparator)).map(File::new).collect(Collectors.toList());
        File found = this.executableFinder.findExecutable(executable, systemPathLocations);
        if (found == null)
            this.logger.debug(String.format("Could not find the executable: %s while searching through: %s", new Object[] { executable, systemPath }));
        return found;
    }
}
