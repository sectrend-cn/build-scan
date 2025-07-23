package com.sectrend.buildscan.executable.impl;

import com.sectrend.buildscan.exception.ExecutableFailedException;
import com.sectrend.buildscan.executable.Exe;
import com.sectrend.buildscan.executable.ExecutionOutput;
import com.sectrend.buildscan.executable.ExecutableRunner;
import com.sectrend.buildscan.executable.ExeRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class SimpleExecutableRunner implements ExecutableRunner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Consumer<String> outputConsumer;

    private final Consumer<String> traceConsumer;

    public SimpleExecutableRunner() {
        this.outputConsumer = this.logger::info;
        this.traceConsumer = this.logger::trace;
    }

    public SimpleExecutableRunner(Consumer<String> outputConsumer, Consumer<String> traceConsumer) {
        this.outputConsumer = outputConsumer;
        this.traceConsumer = traceConsumer;
    }

    public ExecutionOutput execute(File workingDirectory, String exeCmd, String... args) throws ExeRunnerException {
        return execute(new Exe(workingDirectory, new HashMap<>(), exeCmd, Arrays.asList(args)));
    }

    public ExecutionOutput execute(File workingDirectory, String exeCmd, List<String> args) throws ExeRunnerException {
        return execute(new Exe(workingDirectory, new HashMap<>(), exeCmd, args));
    }

    public ExecutionOutput execute(File workingDirectory, File exeFile, String... args) throws ExeRunnerException {
        return execute(new Exe(workingDirectory, new HashMap<>(), exeFile.getAbsolutePath(), Arrays.asList(args)));
    }

    public ExecutionOutput execute(File workingDirectory, File exeFile, List<String> args) throws ExeRunnerException {
        return execute(new Exe(workingDirectory, new HashMap<>(), exeFile.getAbsolutePath(), args));
    }

    public ExecutionOutput execute(Exe exe) throws ExeRunnerException {
        this.logger.info(String.format("Running exe >%s", exe.getMaskedExeDescription()));
        try {
            ProcessBuilder processBuilder = exe.createProcessBuilder();
            Process process = processBuilder.start();
            try (InputStream standardOutputStream = process.getInputStream(); InputStream standardErrorStream = process.getErrorStream(); OutputStream outputStream = process.getOutputStream();) {
                ExecutableStreamThread standardOutputThread = new ExecutableStreamThread(standardOutputStream, this.outputConsumer, this.traceConsumer);
                standardOutputThread.start();
                ExecutableStreamThread errorOutputThread = new ExecutableStreamThread(standardErrorStream, this.outputConsumer, this.traceConsumer);
                errorOutputThread.start();

                // 向子进程发送输入数据（例如 "yes"）
//                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
//                writer.write("yes\n"); // 发送 "yes" 并加上换行符
//                writer.flush();

                int exitCode = process.waitFor();
                this.logger.info("Exe finished: " + exitCode);
                standardOutputThread.join();
                errorOutputThread.join();
                String standardOutput = standardOutputThread.getExecutableOutput().trim();
                String errorOutput = errorOutputThread.getExecutableOutput().trim();
                logger.info("findTaskDependsFile description: " + exe.getMaskedExeDescription() + "》returnCode: " + exitCode + "》errorOutput: " + errorOutput);
                return new ExecutionOutput(exe.getMaskedExeDescription(), exitCode, standardOutput, errorOutput);
            }
        } catch (Exception e) {
            throw new ExeRunnerException(e);
        }
    }

    @Override
    public ExecutionOutput executeSuccessfully(Exe exe) throws ExecutableFailedException {
        {
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

    public ExecutionOutput execute(String[] commands) throws ExeRunnerException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            //Process process = Runtime.getRuntime().exec(commands);
            Process process = processBuilder.start();
            try(InputStream standardOutputStream = process.getInputStream(); InputStream standardErrorStream = process.getErrorStream()) {
                ExecutableStreamThread standardOutputThread = new ExecutableStreamThread(standardOutputStream, this.outputConsumer, this.traceConsumer);
                standardOutputThread.start();
                ExecutableStreamThread errorOutputThread = new ExecutableStreamThread(standardErrorStream, this.outputConsumer, this.traceConsumer);
                errorOutputThread.start();
                int exitCode = process.waitFor();
                this.logger.info("Exe finished: " + exitCode);
                standardOutputThread.join();
                errorOutputThread.join();
                String standardOutput = standardOutputThread.getExecutableOutput().trim();
                String errorOutput = errorOutputThread.getExecutableOutput().trim();
                ExecutionOutput output = new ExecutionOutput(exitCode, standardOutput, errorOutput);
                return output;
            }
        } catch (Exception e) {
            throw new ExeRunnerException(e);
        }
    }
}
