
package com.sectrend.buildscan.result.impl;

import com.sectrend.buildscan.result.ScannableResult;

public class PassedScannableResult implements ScannableResult {
    public PassedScannableResult() {
    }

    public boolean getPassed() {
        return true;
    }
}
