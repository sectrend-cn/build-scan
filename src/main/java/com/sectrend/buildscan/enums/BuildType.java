package com.sectrend.buildscan.enums;


import org.apache.commons.lang3.StringUtils;

public enum BuildType {

    GOMOD_BUILD(1,"goMod", "go.mod", false),
    MVN_BUILD(3,"mvn", "pom.xml", false),
    PIPENV_BUILD(4,"pipenv", "Pipfile|Pipfile.lock", false),
    PIP_BUILD(5,"pip", "requirements.txt|setup.py", false),
    MVN_TEXT_BUILD(7,"mvnText", "", false),
    GODEP_BUILD(16,"goDep","Gopkg.lock", false),
    GO_VENDOR_BUILD_TYPE(24, "goVendor", "vendor.json", false),

    ;


    private int index;
    // 构建类型
    private String buildType;
    // 依赖文件名称, 多个依赖文件以 '|' 隔开
    private String buildFileNames;
    // true: 依赖文件需要全部匹配上  false: 任意匹配上一个依赖文件即可
    private boolean isMatchingAll;

    BuildType(int index ,String buildType, String buildFileNames, boolean isMatchingAll) {
        this.index = index;
        this.buildType = buildType;
        this.buildFileNames = buildFileNames;
        this.isMatchingAll = isMatchingAll;
    }

    public int getIndex() {
        return index;
    }

    public String getBuildType() {
        return buildType;
    }

//    public boolean isNpmType() {
//        return buildType == NPM_BUILD.getBuildType();
//    }
//
//    public boolean isYarnType() {
//        return buildType == YARN_BUILD.getBuildType();
//    }

    public String getBuildFileNames() {
        return buildFileNames;
    }

    public boolean isMatchingAll() {
        return isMatchingAll;
    }

    /**
     * 通过value取枚举
     * @param buildType
     * @return
     */
    public static BuildType getTypeByValue(String buildType){
        if (StringUtils.isBlank(buildType)){
            return null;
        }
        for (BuildType enums : BuildType.values()) {
            if (enums.getBuildType().equals(buildType)) {
                return enums;
            }
        }
        return null;
    }

}
