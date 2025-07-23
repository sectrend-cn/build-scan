package com.sectrend.buildscan.executable.finder;

import com.sectrend.buildscan.buildTools.ScannableException;

import java.io.File;

public interface PipBuilderFinder {
    File findPipBuilder() throws ScannableException;
}
