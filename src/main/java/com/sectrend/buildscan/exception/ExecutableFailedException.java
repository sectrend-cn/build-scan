package com.sectrend.buildscan.exception;

import com.sectrend.buildscan.executable.Exe;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExeRunnerException;

public class ExecutableFailedException extends Exception {
    private static final long serialVersionUID = -4117278710469900787L;

    private final int returnCode;

    private final String executableDescription;

    private final ExeRunnerException executableException;

    public ExecutableFailedException(Exe exe, ExeRunnerException executableException) {
        super("An exception occurred running an exe.", (Throwable)executableException);
        this.executableException = executableException;
        this.returnCode = 0;
        this.executableDescription = exe.getExeDescription();
    }

    public ExecutableFailedException(Exe exe, ExecutionOutput executionOutput) {
        super("An exe returned a non-zero exit code: " + executionOutput.getExitCode());
        this.returnCode = executionOutput.getExitCode();
        this.executableDescription = exe.getExeDescription();
        this.executableException = null;
    }

    public boolean hasReturnCode() {
        return (this.returnCode != 0);
    }

    public int getReturnCode() {
        return this.returnCode;
    }

    public String getExecutableDescription() {
        return this.executableDescription;
    }

    public ExeRunnerException getExecutableException() {
        return this.executableException;
    }
}
