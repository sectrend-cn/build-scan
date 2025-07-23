
package com.sectrend.buildscan.executable.impl;

import java.io.File;

public class SimpleLocalExecutableFinder {
    private final SimpleExecutableFinder simpleExecutableFinder;

    public SimpleLocalExecutableFinder(SimpleExecutableFinder simpleExecutableFinder) {
        this.simpleExecutableFinder = simpleExecutableFinder;
    }

    public File findExecutable(String executableType, File location) {
        return this.simpleExecutableFinder.findExecutable(executableType, location);
    }
}
