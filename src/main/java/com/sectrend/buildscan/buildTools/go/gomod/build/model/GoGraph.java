package com.sectrend.buildscan.buildTools.go.gomod.build.model;

import com.sectrend.buildscan.utils.NameVersion;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoGraph {
    private final NameVersion parent;
    private final NameVersion child;
}
