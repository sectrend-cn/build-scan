package com.sectrend.buildscan.buildTools;


import java.io.File;

public class ScanEnvironment {
    private File outputDirectory;

    public ScanEnvironment(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public File getOutputDirectory() {
        return this.outputDirectory;
    }
}
