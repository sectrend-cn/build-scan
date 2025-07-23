package com.sectrend.buildscan.buildTools;


import com.sectrend.buildscan.result.ScannableResult;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class Scannable {

    //该次构建解析任务的基本信息，包含解析文件的扫描路径
    protected ScannableEnvironment environment;

    protected List<File> relevantFiles = new ArrayList<>();

    public Scannable(ScannableEnvironment environment) {
        this.environment = environment;
    }

    //某些场景需要判断该语言是否有用于构建的可执行文件，不需要判断的场合直接返回 new PassedScannableResult()
    public abstract ScannableResult exeFind() throws ScannableException;

    //用来判断需要的文件是否存在
    public abstract ScannableResult fileFind();

    //提取特定类型的构建的依赖，依赖树一般放在codeLocations字段中
    public abstract ScanResults scanExecute(ScanEnvironment paramScanEnvironment) throws Exception;
}