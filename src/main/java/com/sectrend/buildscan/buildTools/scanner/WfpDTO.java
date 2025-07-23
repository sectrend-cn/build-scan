package com.sectrend.buildscan.buildTools.scanner;

import lombok.Data;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2023/11/24 14:11
 **/
@Data
public class WfpDTO {
    private Long fileSize;
    private String md5;
    private WinDto winDto;
    private WIncalDTO wIncalDTO;
    private byte[] firstBuffer;
    private boolean skipCrc = false;
    private Boolean skipHpsm = false;
    private Boolean isBinary = false;
    private Boolean isSourceCodeFilter = false;
}
