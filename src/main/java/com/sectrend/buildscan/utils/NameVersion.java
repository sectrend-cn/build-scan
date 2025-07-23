package com.sectrend.buildscan.utils;

import lombok.Data;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 14:03
 */
@Data
public class NameVersion extends Baseable {
    private String name;

    private String version;

    public NameVersion(String name, String version) {
        this.name = name;
        this.version = version;
    }

}