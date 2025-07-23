package com.sectrend.buildscan.workflow.maintenance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import com.sectrend.buildscan.workflow.file.DirectoryManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class MaintenanceSystem {

    private static final Logger logger = LoggerFactory.getLogger(MaintenanceSystem.class);

    private LogLevel logLevel;

    private volatile static MaintenanceSystem maintenanceSystem;

    public MaintenanceSystem(String logLevel){

        this.logLevel = fromString(logLevel);
        init();
    }

    public static MaintenanceSystem createDiagnosticSystem(String logLevel){
        if (maintenanceSystem == null) {
            synchronized (MaintenanceSystem.class) {
                if (maintenanceSystem == null) {
                    maintenanceSystem = new MaintenanceSystem(logLevel);
                }
            }
        }
        return maintenanceSystem;
    }

    private void init(){
        DirectoryManager directoryManager = DirectoryManager.getDirectoryManager();
        File file = directoryManager.getLogOutputDirectory();
        File logFile = new File(file, this.logLevel.name() + ".log");
        printOutPutBanner(logFile);
        MaintenanceLogger maintenanceLogger = new MaintenanceLogger(logFile, Level.toLevel(this.logLevel.name()));
        setConsoleLogLevel();
        maintenanceLogger.startLogging();

        //this.logger.info("Output directory: " + directoryManager.getRunsOutputDirectory().getAbsolutePath());
        this.logger.info("Output directory: " + directoryManager.getRunHomeDirectory().getAbsolutePath());
    }

    /**
     * 修改控制台日志输出级别
     */
    private void setConsoleLogLevel() {
        try {
            for (Iterator<Appender<ILoggingEvent>> it = MaintenanceLogUtil.getRootLogger().iteratorForAppenders(); it.hasNext(); ) {
                Appender<ILoggingEvent> appender = it.next();
                if (appender.getName() != null && "CONSOLE".equals(appender.getName())) {
                    // 移除原有的过滤器
                    appender.clearAllFilters();

                    // 添加新的过滤器
                    appender.addFilter(new Filter<ILoggingEvent>() {
                        @Override
                        public FilterReply decide(ILoggingEvent event) {
                            if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
                                StackTraceElement[] stackTraceElements = event.getCallerData();
                                if (stackTraceElements != null && stackTraceElements.length > 0) {
                                    // 获取堆栈信息
                                    StringBuilder stackTraceBuilder = new StringBuilder();
                                    for (StackTraceElement stackTraceElement : stackTraceElements) {
                                        stackTraceBuilder.append(stackTraceElement.toString()).append(System.lineSeparator());
                                    }

                                    if(StringUtils.isNotBlank(stackTraceBuilder.toString())){
                                        modifyLogMessage(event, event.getMessage());
                                    }

                                }
                            }
                            return FilterReply.NEUTRAL;
                        }
                    });
                    appender.start();
                }
            }
        } catch (Exception e) {
            this.logger.error("Adjusting log level failed" + e);
        }
    }

    /**
     * 反射修改堆栈信息
     * @param event
     * @param modifiedMessage
     */
    public static void modifyLogMessage(ILoggingEvent event, String modifiedMessage) {
        try {
            Class<?> loggingEventClass = ch.qos.logback.classic.spi.LoggingEvent.class;

            //获取私有字段
            Field privateField = loggingEventClass.getDeclaredField("throwableProxy");

            privateField.setAccessible(true);

            privateField.set(event, null);

        } catch (Exception e) {

        }
    }


    /**
     * 打印 banner 文件
     * @param logFile
     */
    private void printOutPutBanner(File logFile){

        try {
            InputStream bannerFileStream = getClass().getResourceAsStream(String.format("/%s", new Object[] { "banner.txt" }));
            String bannerFileStreamStr = IOUtils.toString(bannerFileStream, StandardCharsets.UTF_8);
            FileUtils.write(logFile, bannerFileStreamStr, StandardCharsets.UTF_8);
        } catch (Exception e) {}

    }

    /**
     * 获取日志级别 枚举
     * @param level
     * @return
     */
    public LogLevel fromString(String level) {
        if (StringUtils.isNotBlank(level)){
            try {
                return LogLevel.valueOf(level.toUpperCase());
            } catch (IllegalArgumentException illegalArgumentException) {
                logger.warn("Please enter the log level according to the specifications. The default log level is INFO");
            }
        }
        return LogLevel.INFO;
    }


    /**
     * 设置日志级别
     * @param level
     */
    /*public void setLogLevel(String level){
        if (StringUtils.isNotBlank(level)){
            try {
                loggingSystem.setLogLevel("com.sectrend.buildscan", fromString(level));
            } catch (Exception e) {
                logger.error("日志级别设置失败，" + e.getLocalizedMessage());
            }
        }
    }*/


}
