
package com.sectrend.buildscan.executable;

import java.util.Arrays;
import java.util.List;

public class ExecutionOutput {
    private int exitCode = 0;

    private final String standardOutput;

    private final String exceptionOutput;

    private final String cmdDescription;

    public ExecutionOutput(String cmdDescription, int exitCode, String standardOutput, String exceptionOutput) {
        this.cmdDescription = cmdDescription;
        this.exitCode = exitCode;
        this.standardOutput = standardOutput;
        this.exceptionOutput = exceptionOutput;
    }

    public ExecutionOutput(int exitCode, String standardOutput, String exceptionOutput) {
        this.cmdDescription = "";
        this.exitCode = exitCode;
        this.standardOutput = standardOutput;
        this.exceptionOutput = exceptionOutput;
    }

    public List<String> getStandardOutputAsList() {
        return Arrays.asList(this.standardOutput.split(System.lineSeparator()));
    }

    public String getStandardOutput() {
        return this.standardOutput;
    }

    public String getExceptionOutput() {
        return this.exceptionOutput;
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public String getCmdDescription() {
        return this.cmdDescription;
    }
}
