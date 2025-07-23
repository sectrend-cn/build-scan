
package com.sectrend.buildscan.model;

import org.apache.commons.lang3.StringUtils;

public class Dependency extends DependencyId {
    private int line = 0;

    private String group;
    private String name;

    private String version;

    private String newVersion;

    private String completeVariableName;

    // 变量使用类型  version(版本), dependency（整个依赖都是变量）
    private String variableUseType;

    private ForeignId foreignId;

    public Dependency(String name, String version, ForeignId foreignId) {
        this.name = name;
        this.version = version;
        this.foreignId = foreignId;
    }

    public Dependency(String name, ForeignId foreignId) {
        this(name, null, foreignId);
    }

    public Dependency(ForeignId foreignId) {
        this(null, foreignId);
    }

    public int getLine() { return this.line; }
    public void setLine(int line) { this.line = line; }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ForeignId getForeignId() {
        return this.foreignId;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public void setForeignId(ForeignId foreignId) {
        this.foreignId = foreignId;
    }

    public String getCompleteVariableName() {
        return completeVariableName;
    }

    public void setCompleteVariableName(String completeVariableName) {
        this.completeVariableName = completeVariableName;
    }

    public String getVariableUseType() {
        return variableUseType;
    }

    public void setVariableUseType(String variableUseType) {
        this.variableUseType = variableUseType;
    }


    public String toStringForeignIds() {
        StringBuilder sb = new StringBuilder();

        // 包名
        if (StringUtils.isNotBlank(foreignId.getGroup())) {
            sb.append(foreignId.getGroup());
        }
        if (StringUtils.isNotBlank(foreignId.getName())) {
            if(sb.length() > 0)
                sb.append("__");

            sb.append(foreignId.getName());
        }
        if (StringUtils.isNotBlank(foreignId.getVersion())) {
            sb.append("__");
            if (StringUtils.isNotBlank(this.newVersion)) {
                sb.append(this.newVersion);
            } else {
                sb.append(foreignId.getVersion());
            }
        }

        if (line > 0) {
            sb.append("%%" + line);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.name);
        stringBuilder.append("_");
        stringBuilder.append(this.version);
        stringBuilder.append("_");
        if (StringUtils.isNotBlank(this.foreignId.getPrefix())) {
            stringBuilder.append(this.foreignId.getPrefix());
        }
        return stringBuilder.toString();
    }

}