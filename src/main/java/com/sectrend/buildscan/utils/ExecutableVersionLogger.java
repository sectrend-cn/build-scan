package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.executable.ExecutableRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

public class ExecutableVersionLogger {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ExecutableRunner executableRunner;

    public ExecutableVersionLogger(ExecutableRunner executableRunner) {
        this.executableRunner = executableRunner;
    }

    public void log(File projectDir, File executableTarget) {
        log(projectDir, executableTarget, "--version");
    }

    public void log(File projectDir, File executableTarget, String versionArgument) {
        log(() -> this.executableRunner.execute(ExecutableUtils.createFromTarget(projectDir, executableTarget, new String[] { versionArgument })));
    }

    public void log(ToolExecutor showToolVersionExecutor) {
        if (this.logger.isDebugEnabled())
            try {
                showToolVersionExecutor.execute();
            } catch (Exception e) {
                this.logger.debug("Unable to log tool version: {}", e.getMessage());
            }
    }

    public void log(File projectDir, File executableTarget, String versionArgument, Map<String, String> environmentVars) {
        log(() -> {
            executableRunner.execute(
                    ExecutableUtils.createFromTarget(
                            projectDir,
                            environmentVars,
                            executableTarget,
                            Arrays.asList(versionArgument)
                    )
            );
        });
    }

    @FunctionalInterface
    public static interface ToolExecutor {
        void execute() throws Exception;
    }
}
