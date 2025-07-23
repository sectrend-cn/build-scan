package com.sectrend.buildscan.buildTools.scanner;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WinDto {


    private String wfp = "";

    private Integer size = 0;

    private List<Character> normalized = new ArrayList<>();
    private List<Integer> crcLines = new ArrayList<>();
    private List<Integer> crc8List = new ArrayList<>();

    private Integer lastLine = 0;

    private Integer globalIndex = 0;


    private List<String> crcLinesHex = new ArrayList<>();


}
