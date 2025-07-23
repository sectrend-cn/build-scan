package com.sectrend.buildscan.buildTools.scanner;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.sectrend.buildscan.buildTools.scanner.Winnowing.MAX_CRC32;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2023/11/24 14:23
 **/
@Data
public class WIncalDTO {
    private String gram = "";
    private List<Long> window = new ArrayList<>();
    private char normalized = 0;
    private int lastLine = 0;
    private int line = 1;
    private String output = "";

    private long minHash = MAX_CRC32;

    private long lastHash = MAX_CRC32;

    private StringBuilder sb = new StringBuilder();


}
