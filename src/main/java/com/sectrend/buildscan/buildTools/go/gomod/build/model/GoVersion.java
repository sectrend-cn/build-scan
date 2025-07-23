package com.sectrend.buildscan.buildTools.go.gomod.build.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoVersion {
    private final int majorVersion;
    private final int minorVersion;
}
