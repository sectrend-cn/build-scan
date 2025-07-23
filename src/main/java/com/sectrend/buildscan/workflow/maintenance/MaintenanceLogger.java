package com.sectrend.buildscan.workflow.maintenance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.filter.Filter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class MaintenanceLogger {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

    public static final String LOGGER_NAME = "com.sectrend.buildscan";

    private final File logFile;

    private final Level logLevel;

    public MaintenanceLogger(File logFile, Level logLevel) {
        this.logFile = logFile;
        this.logLevel = logLevel;
    }

    /**
     * 设置日志参数
     */
    public void startLogging() {
        try {
            String filePath = this.logFile.getCanonicalPath();
            addAppender(filePath);
        } catch (Exception e) {
            this.logger.error("调整日志配置失败. ", e.getMessage());
        }
    }

    /**
     * 设置 日志文件路径 与 日志级别
     * @param filePath
     * @return
     */
    private FileAppender<ILoggingEvent> addAppender(String filePath) {
        LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        // 设置日志输出格式
        PatternLayoutEncoder ple = new PatternLayoutEncoder();
        ple.setPattern("%d{yyyy-MM-dd HH:mm:ss} %-6p[%thread] --- %m%n%wEx");
        ple.setCharset(Charset.forName("utf8"));
        ple.setContext((Context)lc);
        ple.start();
        FileAppender<ILoggingEvent> appender = new FileAppender();
        // 设置日志输出路径
        appender.setFile(filePath);
        appender.setEncoder((Encoder)ple);
        appender.setContext((Context)lc);
        ThresholdFilter logLevelFilter = new ThresholdFilter();
        // 设置日志输出级别
        logLevelFilter.setName(LOGGER_NAME);
        logLevelFilter.setLevel(this.logLevel.levelStr);
        logLevelFilter.start();
        appender.addFilter((Filter)logLevelFilter);
        appender.start();
        Logger logbackLogger = MaintenanceLogUtil.getOurLogger();
        logbackLogger.addAppender((Appender)appender);
        logbackLogger.setLevel(this.logLevel);
        return appender;
    }

}
