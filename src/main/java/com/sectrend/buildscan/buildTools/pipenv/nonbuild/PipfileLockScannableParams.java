package com.sectrend.buildscan.buildTools.pipenv.nonbuild;

import com.sectrend.buildscan.utils.EnumUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

@AllArgsConstructor
public class PipfileLockScannableParams {
    @Getter
    private final EnumUtil<PipenvDependencyType> dependencyTypeFilter;
    private final String projectName;

    public Optional<String> getPipProjectName() {
        return Optional.ofNullable(this.projectName);
    }
}
