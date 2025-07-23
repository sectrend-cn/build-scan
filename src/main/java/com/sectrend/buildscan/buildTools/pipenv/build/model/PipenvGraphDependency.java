package com.sectrend.buildscan.buildTools.pipenv.build.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PipenvGraphDependency {
    private final List<PipenvGraphDependencyEntry> entries;
}
