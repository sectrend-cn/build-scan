package com.sectrend.buildscan.buildTools.pipenv.build.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
public class PipenvFreeze {
    private final List<PipenvFreezeDependencyEntry> entries;
}
