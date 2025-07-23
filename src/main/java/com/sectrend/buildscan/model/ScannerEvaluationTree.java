package com.sectrend.buildscan.model;

import java.io.File;
import java.util.List;
import java.util.Set;

public class ScannerEvaluationTree {

    private final File directory;

    private final int dept;

    private final List<String> buildTypes;

    // private final ExtractHandler extractHandler;

    private final Set<ScannerEvaluationTree> children;

    public ScannerEvaluationTree(File directory, int dept, List<String> buildTypes, Set<ScannerEvaluationTree> children) {
        this.directory = directory;
        this.dept = dept;
        this.buildTypes = buildTypes;
        this.children = children;
    }

    public File getDirectory() {
        return directory;
    }

    public int getDept() {
        return dept;
    }

    public List<String> getBuildTypes() {
        return buildTypes;
    }

    public Set<ScannerEvaluationTree> getChildren() {
        return children;
    }
}
