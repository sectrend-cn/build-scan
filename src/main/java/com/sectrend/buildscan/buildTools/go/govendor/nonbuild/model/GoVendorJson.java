package com.sectrend.buildscan.buildTools.go.govendor.nonbuild.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GoVendorJson {
    //关于这个VendorJson对象的注释或说明
    @SerializedName("comment")
    private final String goVendorComment;
    //指定在处理依赖包时应该忽略的模式或规则
    @SerializedName("ignore")
    private final String ignore;
    //表示Go语言项目的依赖包
    @SerializedName("package")
    private final List<PackageInfo> packages;
    //项目的根路径
    @SerializedName("rootPath")
    private final String rootPath;
}
