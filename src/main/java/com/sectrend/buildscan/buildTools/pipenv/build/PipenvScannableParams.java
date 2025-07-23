package com.sectrend.buildscan.buildTools.pipenv.build;

import lombok.Getter;

import java.util.Optional;

public class PipenvScannableParams {

    @Getter
    private final boolean pipenvProjectTreeOnly;

    private final String pipenvProjectName;

    private final String pipenvProjectVersionName;

    public PipenvScannableParams(String pipenvProjectName, String pipenvProjectVersionName, boolean pipenvProjectTreeOnly) {
        this.pipenvProjectName = pipenvProjectName;
        this.pipenvProjectVersionName = pipenvProjectVersionName;
        this.pipenvProjectTreeOnly = pipenvProjectTreeOnly;
    }

    public Optional<String> getPipenvProjectName() {
        return Optional.ofNullable(this.pipenvProjectName);
    }

    public Optional<String> getPipenvProjectVersionName() {
        return Optional.ofNullable(this.pipenvProjectVersionName);
    }
}
