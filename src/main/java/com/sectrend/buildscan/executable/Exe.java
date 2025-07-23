package com.sectrend.buildscan.executable;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class Exe {
    private final File workingDirectory;

    private final Map<String, String> envVariables = new HashMap<>();

    private final List<String> command = new ArrayList<>();

    private final List<String> commandAndArguments = new ArrayList<>();

    public Exe(File workingDirectory, Map<String, String> envVariables, List<String> command) {
        this.workingDirectory = workingDirectory;
        this.envVariables.putAll(envVariables);
        this.command.addAll(command);
    }

    public Exe(File workingDirectory, Map<String, String> envVariables, String exeCmd, List<String> executableArguments) {
        this.workingDirectory = workingDirectory;
        if (envVariables != null)
            this.envVariables.putAll(envVariables);
        this.command.add(exeCmd);
        this.command.addAll(executableArguments);
    }

    public static Exe create(File workingDirectory, Map<String, String> envVariables, String command, List<String> arguments) {
        List<String> commandAndArguments = new ArrayList<>();
        commandAndArguments.add(command);
        commandAndArguments.addAll(arguments);
        return new Exe(workingDirectory, envVariables, commandAndArguments);
    }

    /**
     *
     * @param workingDirectory 父路径
     * @param command 相对路径
     * @param arguments 参数
     * @return
     */
    public static Exe create(File workingDirectory, String command, List<String> arguments) {
        return create(workingDirectory, Collections.emptyMap(), command, arguments);
    }

    private List<String> createProcessBuilderArguments() {
        List<String> processBuilderArguments = new ArrayList<>(this.command);
        return processBuilderArguments;
    }

    public ProcessBuilder createProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(this.workingDirectory);
        processBuilder.command(this.command);
        Map<String, String> processBuilderEnvironment = processBuilder.environment();
        for (Map.Entry me : this.envVariables.entrySet()) {
            populateEnvironmentMap(processBuilderEnvironment, me.getKey(), me.getValue());
        }
        return processBuilder;
    }

    private void populateEnvironmentMap(Map<String, String> environment, Object key, Object value) {
        if (key != null && value != null) {
            String keyString = key.toString();
            String valueString = value.toString();
            if (keyString != null && valueString != null)
                environment.put(keyString, valueString);
        }
    }

    public File getWorkingDirectory() {
        return this.workingDirectory;
    }

    public Map<String, String> getEnvVariables() {
        return this.envVariables;
    }

    public List<String> getCommand() {
        return this.command;
    }

    public String getMaskedExeDescription() {
        List<String> arguments = new ArrayList<>();
        for (String argument : createProcessBuilderArguments()) {
            if (argument.matches(".*password.*=.*")) {
                String maskedArgument = argument.substring(0, argument.indexOf('=') + 1) + "********";
                arguments.add(maskedArgument);
                continue;
            }
            arguments.add(argument);
        }
        return StringUtils.join(arguments, ' ');
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Exe that = (Exe) obj;
        return Objects.equals(getWorkingDirectory(), that.getWorkingDirectory()) &&
                Objects.equals(envVariables, that.envVariables) &&
                Objects.equals(getCommand(), that.getCommand());
    }

    public int hashCode() {
        return Objects.hash(getWorkingDirectory(), envVariables, getCommand());
    }

    public String getExeDescription() {
        return getMaskedCmd(this.commandAndArguments);
    }


    public static String getMaskedCmd(List<String> commandWithArguments) {
        List<String> pieces = new ArrayList<>();
        for (String argument : commandWithArguments) {
            if (argument.matches(".*password.*=.*")) {
                String maskedArgument = argument.substring(0, argument.indexOf('=') + 1) + "********";
                pieces.add(maskedArgument);
                continue;
            }
            pieces.add(argument);
        }
        return StringUtils.join(pieces, ' ');
    }
}
