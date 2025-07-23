package com.sectrend.buildscan.utils;

import java.util.*;

public class EnumUtil<T extends Enum<T>>{

    private final Set<T> excludedSet;

    public static <T extends Enum<T>> EnumUtil<T> fromExcluded(Set<T> excludedSet) {
        return new EnumUtil<>(excludedSet);
    }

    private EnumUtil(Set<T> excludedSet) {
        this.excludedSet = excludedSet;
    }

    public boolean include(T enumValue) {
        return !exclude(enumValue);
    }

    public boolean exclude(T enumValue) {
        return this.excludedSet.contains(enumValue);
    }
}
