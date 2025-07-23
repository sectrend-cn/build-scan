package com.sectrend.buildscan.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @Description
 * @Author yang.zhang
 * @Date 2024/4/22 13:33
 **/
@Data
@AllArgsConstructor
public class SerialLicense {
    private String spdxId;

    private String content;

    private int length;
}
