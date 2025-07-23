
package com.sectrend.buildscan.workflow.file;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class DirectoryParams {


    private final Path outputPath;


    public DirectoryParams(Path outputPath) throws IOException {
        this.outputPath = toRealPath(outputPath);
    }

    public Optional<Path> getOutputPathOverride() {
        return Optional.ofNullable(this.outputPath);
    }

    @Nullable
    private Path toRealPath(@Nullable Path rawPath) throws IOException {
        if (rawPath == null)
            return null;
        return rawPath.toAbsolutePath();
    }

}
