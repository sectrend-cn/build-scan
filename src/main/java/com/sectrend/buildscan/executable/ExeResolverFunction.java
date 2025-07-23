package com.sectrend.buildscan.executable;

import com.sectrend.buildscan.buildTools.ScannableException;

import java.io.File;

@FunctionalInterface
public interface ExeResolverFunction {
    File resolve() throws ScannableException;
}
