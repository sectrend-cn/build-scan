package com.sectrend.buildscan.result.impl;

import com.sectrend.buildscan.result.ScannableResult;

/**
 * <p>
 *     没有检测到可执行的构建文件
 * </p>
 *
 * @author yhx
 * @date 2022/6/7 15:00
 */
public class FailedScannableResult implements ScannableResult {
    public boolean getPassed() {
        return false;
    }
}