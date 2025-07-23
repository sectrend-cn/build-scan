package com.sectrend.buildscan.buildTools;


import com.sectrend.buildscan.base.graph.DependencyLocation;
import com.sectrend.buildscan.utils.NameVersion;

import java.io.File;
import java.util.*;

public class ScanResults {
    private final List<DependencyLocation> dependencyLocations;

    private final List<File> relevantFiles;

    private final List<File> unrecognizedFiles;

    private final ScanExecuteStatus scanExecuteStatus;

    private final Exception exception;

    private final String scanDescription;

    private final String scanProjectVersion;

    private final String scanProjectName;

    private final Map<ScanMetadata, Object> metaDataMap;

    //true为构建，false为非构建
    private boolean buildFlag;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String fileName;

    public void setBuildFlag(boolean buildFlag) {
        this.buildFlag = buildFlag;
    }

    public boolean getBuildFlag(){
        return this.buildFlag;
    }


    public static ScanResults success(DependencyLocation dependencyLocation) {
        return new ScanResults.Builder().success(dependencyLocation).build();
    }

    public static ScanResults success(List<DependencyLocation> dependencyLocations) {
        return new ScanResults.Builder().success(dependencyLocations).build();
    }

    private ScanResults(Builder builder) {
        this.dependencyLocations = builder.dependencyLocations;
        this.scanExecuteStatus = builder.scanExecuteStatus;
        this.exception = builder.exception;
        this.scanDescription = builder.scanDescription;
        this.scanProjectVersion = builder.scanProjectVersion;
        this.scanProjectName = builder.scanProjectName;
        this.metaDataMap = builder.metaDataMap;
        this.relevantFiles = builder.relevantFiles;
        this.unrecognizedFiles = builder.unrecognizedFiles;
        if (this.scanExecuteStatus == null)
            throw new IllegalArgumentException("An extraction requires a result type.");
    }

    public <T> Optional<T> getMetaData(ScanMetadata<T> scanMetadata) {
        if (this.metaDataMap.containsKey(scanMetadata)) {
            Class<T> clazz = scanMetadata.getMetadataClass();
            Object value = this.metaDataMap.get(scanMetadata);
            if (value != null && clazz.isAssignableFrom(value.getClass()))
                return Optional.of(clazz.cast(value));
            return Optional.empty();
        }
        return Optional.empty();
    }

    public boolean isSuccess() {
        return (this.scanExecuteStatus == ScanExecuteStatus.SUCCESS);
    }

    public List<DependencyLocation> getDependencyLocations() {
        return this.dependencyLocations;
    }

    public Exception getException() {
        return this.exception;
    }

    public String getScanDescription() {
        return this.scanDescription;
    }

    public String getScanProjectVersion() {
        return this.scanProjectVersion;
    }

    public String getScanProjectName() {
        return this.scanProjectName;
    }

    public ScanExecuteStatus getScanExecuteStatus() {
        return this.scanExecuteStatus;
    }

    public List<File> getRelevantFiles() {
        return this.relevantFiles;
    }

    public List<File> getUnrecognizedFiles() {
        return this.unrecognizedFiles;
    }

    public void resetCodeLocations(DependencyLocation dependencyLocation) {
        if (this.dependencyLocations != null){
            this.dependencyLocations.clear();
            this.dependencyLocations.add(dependencyLocation);
        }
    }

    public static class Builder {
        private final List<DependencyLocation> dependencyLocations = new ArrayList<>();

        private final List<File> relevantFiles = new ArrayList<>();

        private final List<File> unrecognizedFiles = new ArrayList<>();

        private ScanExecuteStatus scanExecuteStatus;

        private Exception exception;

        private String scanDescription;

        private String scanProjectVersion;

        private String scanProjectName;

        private final Map<ScanMetadata, Object> metaDataMap = new HashMap<>();

        public Builder scanProjectName(String scanProjectName) {
            this.scanProjectName = scanProjectName;
            return this;
        }

        public Builder scanProjectVersion(String scanProjectVersion) {
            this.scanProjectVersion = scanProjectVersion;
            return this;
        }

        public Builder nameVersionIfPresent(Optional<NameVersion> nameVersion) {
            if (nameVersion.isPresent()) {
                scanProjectName(((NameVersion)nameVersion.get()).getName());
                scanProjectVersion(((NameVersion)nameVersion.get()).getVersion());
            }
            return this;
        }

        public Builder dependencyLocations(DependencyLocation dependencyLocation) {
            this.dependencyLocations.add(dependencyLocation);
            return this;
        }

        public Builder dependencyLocations(List<DependencyLocation> dependencyLocation) {
            this.dependencyLocations.addAll(dependencyLocation);
            return this;
        }

        public Builder success(DependencyLocation dependencyLocation) {
            dependencyLocations(dependencyLocation);
            success();
            return this;
        }

        public Builder success() {
            this.scanExecuteStatus = ScanExecuteStatus.SUCCESS;
            return this;
        }

        public Builder success(List<DependencyLocation> dependencyLocation) {
            dependencyLocations(dependencyLocation);
            success();
            return this;
        }

        public Builder failure(String scanDescription) {
            this.scanExecuteStatus = ScanExecuteStatus.FAILURE;
            this.scanDescription = scanDescription;
            return this;
        }

        public Builder exception(Exception exception) {
            this.scanExecuteStatus = ScanExecuteStatus.EXCEPTION;
            this.exception = exception;
            return this;
        }

        public ScanResults build() {
            return new ScanResults(this);
        }
    }

    public enum ScanExecuteStatus {
        SUCCESS, FAILURE, EXCEPTION;
    }
}
