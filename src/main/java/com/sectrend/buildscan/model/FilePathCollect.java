package com.sectrend.buildscan.model;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/4/7 5:53
 **/
@Data
public class FilePathCollect {

    private List<String> files = new ArrayList<>();

    private List<String> dirs = new ArrayList<>();

    private List<String> projectLicenseFile = new ArrayList<>();

    private List<String> symbolicLinks = new ArrayList<>();

    public boolean isEmpty() {
        return CollectionUtils.isEmpty(files) && CollectionUtils.isEmpty(dirs) && CollectionUtils.isEmpty(symbolicLinks);
    }
}
