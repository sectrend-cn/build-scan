package com.sectrend.buildscan.model;

import lombok.Data;

import java.io.File;

@Data
public class ExecutionDir {

    private File file;

    private Integer depth;

    private String buildType;
}
