package com.sectrend.buildscan.model;

import lombok.Data;
import org.apache.logging.log4j.util.Strings;

import java.util.List;

/**
 * 依赖信息 根节点
 */
@Data
public class DependencyRoot {

    private List<DependencyInfo> dependencyInfoList;

    private boolean buildFlag;

    private String buildSource = Strings.EMPTY;

    private String sourcePath;

}
