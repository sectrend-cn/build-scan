package com.sectrend.buildscan.executable.impl;

import com.sectrend.buildscan.exception.ExecutableFailedException;
import com.sectrend.buildscan.executable.Exe;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExeRunnerException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ScanExecutableRunner extends SimpleExecutableRunner {


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final boolean shouldLogOutput;

    public ScanExecutableRunner(Consumer<String> outputConsumer, Consumer<String> traceConsumer, boolean shouldLogOutput) {
        super(outputConsumer, traceConsumer);
        this.shouldLogOutput = shouldLogOutput;
    }

    public static ScanExecutableRunner newDebug() {
        Logger logger = LoggerFactory.getLogger(SimpleExecutableRunner.class);
        return new ScanExecutableRunner(logger::debug, logger::trace, true);
    }

    public static ScanExecutableRunner newInfo() {
        Logger logger = LoggerFactory.getLogger(SimpleExecutableRunner.class);
        return new ScanExecutableRunner(logger::info, logger::trace,false);
    }

    public ExecutionOutput execute(Exe exe) throws ExeRunnerException {

        ExecutionOutput output = super.execute(exe);
        if (output.getExitCode() != 0 && this.shouldLogOutput && !this.logger.isDebugEnabled() && !this.logger.isTraceEnabled()) {
            if (StringUtils.isNotBlank(output.getStandardOutput())) {
                this.logger.info("Standard Output: ");
                this.logger.info(output.getStandardOutput());
            }
            if (StringUtils.isNotBlank(output.getExceptionOutput())) {
                this.logger.info("Error Output: ");
                this.logger.info(output.getExceptionOutput());
            }
        }
        return output;
    }

    @Override
    public ExecutionOutput executeSuccessfully(Exe exe) throws ExecutableFailedException {
        try {
            ExecutionOutput executionOutput = execute(exe);
            if (executionOutput.getExitCode() != 0) {
                throw new ExecutableFailedException(exe, executionOutput);
            }
            return executionOutput;
        } catch (ExeRunnerException e) {
            throw new ExecutableFailedException(exe, e);
        }
    }
}
