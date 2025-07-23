
package com.sectrend.buildscan.executable.impl;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;
public class ExecutableStreamThread extends Thread {
    private final BufferedReader reader;

    private final StringBuilder outputBuffer;

    private final Consumer<String> logOutput;

    private final Consumer<String> logTrace;

    private String executableOutput;

    public ExecutableStreamThread(InputStream executableStream, Consumer<String> logOutput, Consumer<String> logTrace) {
        super(Thread.currentThread().getName() + "-Executable_Stream_Thread");
        this.logOutput = logOutput;
        this.logTrace = logTrace;
        this.reader = new BufferedReader(new InputStreamReader(executableStream, StandardCharsets.UTF_8));
        this.outputBuffer = new StringBuilder();
    }

    public void run() {
        try {
            String separator = System.lineSeparator();
            String lineContent;
            while ((lineContent = this.reader.readLine()) != null) {
                if (!lineContent.trim().startsWith("Active code page")) {
                    this.outputBuffer.append(lineContent).append(separator);
                }
                this.logOutput.accept(lineContent);
            }
        } catch (IOException e) {
            this.logTrace.accept("Error reading executable stream: " + e.getMessage());
            e.printStackTrace();
        }
        this.executableOutput = this.outputBuffer.toString();
    }

    public String getExecutableOutput() {
        return this.executableOutput;
    }
}