
package com.sectrend.buildscan.system;

import org.apache.commons.lang3.SystemUtils;

public enum OSType {
    LINUX, MAC, WINDOWS;

    public static OSType determineFromSystem() {
        if (SystemUtils.IS_OS_MAC)
            return MAC;
        if (SystemUtils.IS_OS_WINDOWS)
            return WINDOWS;
        return LINUX;
    }

}
