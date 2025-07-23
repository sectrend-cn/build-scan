
package com.sectrend.buildscan.buildTools.go.govendor.nonbuild.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PackageInfo {
    //包的版本修订号（代码提交版本），为哈希值
    @SerializedName("revision")
    private final String revision;
    //修订号对应的时间戳
    @SerializedName("revisionTime")
    private final String revisionTime;
    //包的版本号，标识发布版本
    @SerializedName("version")
    private final String version;
    //包的SHA1校验和
    @SerializedName("checksumSHA1")
    private final String checksumSHA1;
    //包的路径, 可以视为唯一标识符
    @SerializedName("path")
    private final String path;
}
