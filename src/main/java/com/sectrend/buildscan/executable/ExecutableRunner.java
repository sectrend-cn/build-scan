package com.sectrend.buildscan.executable;

import com.sectrend.buildscan.exception.ExecutableFailedException;

import java.io.File;
import java.util.List;

public interface ExecutableRunner {
    ExecutionOutput execute(File paramFile, String paramString, String... paramVarArgs) throws ExeRunnerException;

    ExecutionOutput execute(File paramFile, String paramString, List<String> paramList) throws ExeRunnerException;

    ExecutionOutput execute(File paramFile1, File paramFile2, String... paramVarArgs) throws ExeRunnerException;

    ExecutionOutput execute(File paramFile1, File paramFile2, List<String> paramList) throws ExeRunnerException;

    ExecutionOutput execute(Exe paramExe) throws ExeRunnerException;

    ExecutionOutput executeSuccessfully(Exe exe) throws ExecutableFailedException;
}
