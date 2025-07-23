
package com.sectrend.buildscan.workflow;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class ScanRun {

    private final String id;

    public static ScanRun createDefault() {
        Random random = new Random();
        int randomNumber = random.nextInt(1000);
        return new ScanRun(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS").withZone(ZoneOffset.UTC).format(Instant.now().atZone(ZoneOffset.UTC)) + "_" + randomNumber);
    }

    public ScanRun(String id) {
        this.id = id;
    }

    public String getRunId() {
        return this.id;
    }

}
