package com.sectrend.buildscan.executable;

import org.jetbrains.annotations.Nullable;

import java.io.File;

public class ExeTarget {
    @Nullable
    private File fileTarget;

    @Nullable
    private String stringTarget;

    private ExeTarget(@Nullable File fileTarget, @Nullable String stringTarget) {
        this.fileTarget = fileTarget;
        this.stringTarget = stringTarget;
    }

    @Nullable
    public static ExeTarget forFile(@Nullable File targetFile) {
        if (targetFile == null)
            return null;
        return new ExeTarget(targetFile, null);
    }

    @Nullable
    public static ExeTarget forCommand(@Nullable String command) {
        if (command == null)
            return null;
        return new ExeTarget(null, command);
    }

    @Nullable
    public String toCommand() {
        if (this.stringTarget != null)
            return this.stringTarget;
        if (this.fileTarget != null)
            return this.fileTarget.getAbsolutePath();
        return null;
    }
}
