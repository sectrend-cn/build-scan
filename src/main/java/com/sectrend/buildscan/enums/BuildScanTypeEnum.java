package com.sectrend.buildscan.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

@Getter
@AllArgsConstructor
public enum BuildScanTypeEnum {

    NO_EXECUTE(0),
    BUILD(1),
    NON_BUILD(2),
    ALL(3);

    private final Integer value;

    public static BuildScanTypeEnum of(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        return Stream.of(BuildScanTypeEnum.values()).filter(e -> String.valueOf(e.getValue()).equals(code.trim())).findFirst().orElse(null);
    }

}