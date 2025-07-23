package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.executable.Exe;
import com.sectrend.buildscan.executable.ExeTarget;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExecutableUtils {

    public static Exe createFromTarget(File directory, File target, String... commands) {
        return Exe.create(directory, target.getPath(), Arrays.asList(commands));
    }

    public static Exe createFromTarget(File directory, File target, List<String> commands) {
        return Exe.create(directory, target.getPath(), commands);
    }

    /**
     *
     * @param directory
     * @param target 依赖文件的路径与File对象（后者可为空）
     * @param commands
     * @return
     */
    public static Exe createFromTarget(File directory, ExeTarget target, List<String> commands) {
        return Exe.create(directory, target.toCommand(), commands);
    }

    public static Exe createFromTarget(File directory, Map<String, String> environmentVariables, File target, List<String> commands) {
        return Exe.create(directory, environmentVariables, ExeTarget.forFile(target).toCommand(), commands);
    }

    public static Exe createFromTarget(File directory, Map<String, String> environmentVariables, ExeTarget target, List<String> commands) {
        return Exe.create(directory, environmentVariables, target.toCommand(), commands);
    }

    public static Exe createFromTarget(File directory, ExeTarget target, String... commands) {


        return Exe.create(directory, target.toCommand(), Arrays.asList(commands));
    }

}
