package com.sectrend.buildscan.buildTools.git.nonbuild;

import com.sectrend.buildscan.buildTools.ScanResults;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfiguration;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfigurationBranch;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfigurationNode;
import com.sectrend.buildscan.buildTools.git.nonbuild.analyze.GitConfigurationNameVersionConverter;
import com.sectrend.buildscan.buildTools.git.nonbuild.model.GitConfigurationRemote;
import com.sectrend.buildscan.executable.SynthesisException;
import com.sectrend.buildscan.utils.NameVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GitAnalyzeScanExecutor {
    private final static Logger logger = LoggerFactory.getLogger(GitAnalyzeScanExecutor.class);


    private final GitConfigurationNameVersionConverter gitConfigurationNameVersionConverter;


    public GitAnalyzeScanExecutor( GitConfigurationNameVersionConverter gitConfigurationNameVersionConverter) {
        this.gitConfigurationNameVersionConverter = gitConfigurationNameVersionConverter;
    }

    public final ScanResults scanExecute(File gitConfigFile, File gitHeadFile) {
        try {
            String headFileContent = FileUtils.readFileToString(gitHeadFile, StandardCharsets.UTF_8);
            String gitHead = headFileContent.trim().replaceFirst("ref:\\w*", "").trim();
            List<String> configFileContent = FileUtils.readLines(gitConfigFile, StandardCharsets.UTF_8);
            List<GitConfigurationNode> gitConfigurationNodes = analyzeGitConfiguration(configFileContent);
            GitConfiguration gitConfiguration = createGitConfiguration(gitConfigurationNodes);
            NameVersion projectNameVersion = this.gitConfigurationNameVersionConverter.conversionToProjectInfo(gitConfiguration, gitHead);
            return (new ScanResults.Builder())
                    .success()
                    .scanProjectName(projectNameVersion.getName())
                    .scanProjectVersion(projectNameVersion.getVersion())
                    .build();
        } catch (IOException | SynthesisException e) {
            this.logger.debug("Unable to extract project information from git configuration.", e);
            return (new ScanResults.Builder())
                    .success()
                    .build();
        }
    }

    public List<GitConfigurationNode> analyzeGitConfiguration(List<String> gitConfigLines) {
        List<GitConfigurationNode> gitConfigurationNodes = new ArrayList<>();
        List<String> lineBuffer = new ArrayList<>();
        for (String rawLine : gitConfigLines) {
            String line = StringUtils.stripToEmpty(rawLine);
            if (StringUtils.isBlank(line))
                continue;
            if (line.startsWith("[") && line.endsWith("]")){
                Optional<GitConfigurationNode> gitConfigNode = processGitConfigurationNodeLines(lineBuffer);
                gitConfigNode.ifPresent(gitConfigurationNodes::add);
                lineBuffer.clear();
            }
            lineBuffer.add(line);
        }
        processGitConfigurationNodeLines(lineBuffer).ifPresent(gitConfigurationNodes::add);
        return gitConfigurationNodes;
    }

    private Optional<GitConfigurationNode> processGitConfigurationNodeLines(List<String> lineList) {
        Map<String, String> properties = new HashMap<>();
        String nodeName = null;
        String nodeType = null;

        for (String line : lineList) {
            if (isNodeStartLine(line)) {
                String[] nodeInfoParts = parseNodeStartLine(line);
                if (nodeInfoParts != null) {
                    nodeType = nodeInfoParts[0];
                    if (nodeInfoParts.length == 2) {
                        nodeName = nodeInfoParts[1];
                    }
                } else {
                    this.logger.warn("The git config node is invalid. Skipping: " + line);
                    break;
                }
                continue;
            }

            // Process key-value pairs
            if (isValidPropertyLine(line)) {
                String[] propertyPair = line.split("=");
                String propertyKey = propertyPair[0].trim();
                String propertyValue = propertyPair[1].trim();
                properties.put(propertyKey, propertyValue);
                continue;
            }

            this.logger.warn("The git config node's attribute is invalid. Skipping: " + line);
        }

        return StringUtils.isNotBlank(nodeType) ? Optional.of(new GitConfigurationNode(nodeType, nodeName, properties)) : Optional.empty();
    }

    private boolean isNodeStartLine(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private String[] parseNodeStartLine(String line) {
        String lineWithoutBrackets = line.substring(1, line.length() - 1).trim();
        String[] nodeInfoParts = lineWithoutBrackets.split(" ");
        if (nodeInfoParts.length == 1 || nodeInfoParts.length == 2) {
            if (nodeInfoParts.length == 2) {
                nodeInfoParts[1] = nodeInfoParts[1].replace("\"", "").trim();
            }
            return nodeInfoParts;
        }
        return null;
    }

    private boolean isValidPropertyLine(String line) {
        return line.contains("=") && line.split("=").length == 2;
    }

    public GitConfiguration createGitConfiguration(List<GitConfigurationNode> gitConfigurationNodes) {
        List<GitConfigurationRemote> gitConfigurationRemotes = gitConfigurationNodes.stream().filter(node -> node.getNodeType().equals("remote")).map(node -> {
            Optional<String> url = node.getProperty("url");
            String s = url.get();
            String remoteNodeName = node.getNodeName().get();
            String remoteNodeUrl = node.getProperty("url").get();
            String remoteNodeFetch = node.getProperty("fetch").get();
            return new GitConfigurationRemote(remoteNodeName, remoteNodeUrl, remoteNodeFetch);
        }).collect(Collectors.toList());
        List<GitConfigurationBranch> gitConfigurationBranches = gitConfigurationNodes.stream().filter(node -> node.getNodeType().equals("branch")).map(node -> {
            String remoteNodeName =  node.getNodeName().orElse("");
            String remoteNodeRemote = node.getProperty("remote").orElse("");
            String remoteNodeMerge =  node.getProperty("merge").orElse("");
            return new GitConfigurationBranch(remoteNodeName, remoteNodeRemote, remoteNodeMerge);
        }).collect(Collectors.toList());
        return new GitConfiguration( gitConfigurationBranches,gitConfigurationRemotes);
    }
}
