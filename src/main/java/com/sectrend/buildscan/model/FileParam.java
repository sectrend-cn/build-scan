package com.sectrend.buildscan.model;

import lombok.Data;

@Data
public class FileParam {

    private String fileName;
    private String fileText;

    public FileParam(String fileName, String fileText) {
        this.fileName = fileName;
        this.fileText = fileText;
    }
}
