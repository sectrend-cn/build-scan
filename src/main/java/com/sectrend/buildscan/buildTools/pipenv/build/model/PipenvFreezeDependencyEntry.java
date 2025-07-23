package com.sectrend.buildscan.buildTools.pipenv.build.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PipenvFreezeDependencyEntry {

    private final String name;

    private final String version;
}
