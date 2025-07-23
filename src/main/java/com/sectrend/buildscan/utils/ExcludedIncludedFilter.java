package com.sectrend.buildscan.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:52
 */
public class ExcludedIncludedFilter {
    protected final Set<String> excludedSet;

    protected final Set<String> includedSet;

    public ExcludedIncludedFilter(String toExclude, String toInclude) {
        this.excludedSet = createSetFromString(toExclude);
        this.includedSet = createSetFromString(toInclude);
    }

    public ExcludedIncludedFilter(Collection<String> toExcludeList, Collection<String> toIncludeList) {
        this.excludedSet = new HashSet<>(toExcludeList);
        this.includedSet = new HashSet<>(toIncludeList);
    }

    public boolean willExclude(String itemName) {
        return this.excludedSet.contains(itemName);
    }

    public boolean willInclude(String itemName) {
        return this.includedSet.isEmpty() || this.includedSet.contains(itemName);
    }

    public final boolean shouldInclude(String itemName) {
        if (willExclude(itemName))
            return false;
        return willInclude(itemName);
    }

    private Set<String> createSetFromString(String s) {
        Set<String> set = new HashSet<>();
        StringTokenizer stringTokenizer = new StringTokenizer(StringUtils.trimToEmpty(s), ",");
        while (stringTokenizer.hasMoreTokens())
            set.add(StringUtils.trimToEmpty(stringTokenizer.nextToken()));
        return set;
    }
}
