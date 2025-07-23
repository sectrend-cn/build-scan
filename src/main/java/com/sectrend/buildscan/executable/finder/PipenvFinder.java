package com.sectrend.buildscan.executable.finder;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;

import java.io.File;

public interface PipenvFinder {
    File findPipenv(ScannableEnvironment paramScannableEnvironment) throws ScannableException;
}
