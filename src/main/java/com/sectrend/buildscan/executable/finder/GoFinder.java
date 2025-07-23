package com.sectrend.buildscan.executable.finder;

import com.sectrend.buildscan.buildTools.ScannableEnvironment;
import com.sectrend.buildscan.buildTools.ScannableException;

import java.io.File;

public interface GoFinder {

    File findGo(ScannableEnvironment paramDetectableEnvironment) throws ScannableException;

}
