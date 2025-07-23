package com.sectrend.buildscan.workflow.maintenance;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

public class MaintenanceLogUtil {

    public static Logger getLogger(String named) {
        Logger logger = (Logger) LoggerFactory.getLogger(named);
        return logger;
    }

    public static Logger getOurLogger() {
        return getLogger(MaintenanceLogger.LOGGER_NAME);
    }

    public static Logger getRootLogger() {
        return getLogger("ROOT");
    }

}
