package com.sectrend.buildscan.executable.finder;

import com.sectrend.buildscan.buildTools.ScannableException;

import java.io.File;

public interface GitFinder {
    File findGit() throws ScannableException;
}
