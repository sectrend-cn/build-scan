package com.sectrend.buildscan.buildTools.pip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@Data
@AllArgsConstructor
public class PipBuildScannableParams {
    private final String pipProjectName;

    @Getter
    private final Set<Path> requirementsPaths;

    public Optional<String> getPipProjectName() {
        return Optional.ofNullable(this.pipProjectName);
    }

}
