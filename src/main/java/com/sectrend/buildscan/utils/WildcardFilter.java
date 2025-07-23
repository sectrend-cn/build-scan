package com.sectrend.buildscan.utils;

import org.apache.commons.io.FilenameUtils;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.Set;

/**
 * @author yhx
 * @date 2022/6/8 13:53
 */

public class WildcardFilter extends ExcludedIncludedFilter {
    public WildcardFilter(String toExclude, String toInclude) {
        super(toExclude, toInclude);
    }

    public static WildcardFilter fromCollections(Collection<String> toExclude, Collection<String> toInclude) {
        return new WildcardFilter(toExclude, toInclude);
    }

    protected WildcardFilter(Collection<String> toExcludeList, Collection<String> toIncludeList) {
        super(toExcludeList, toIncludeList);
    }

    public boolean willExclude(String itemName) {
        return setContains(itemName, this.excludedSet, x -> super.willExclude(x));
    }

    public boolean willInclude(String itemName) {
        return setContains(itemName, this.includedSet, x -> super.willInclude(x));
    }

    private boolean setContains(String itemName, Set<String> tokenSet, Predicate<String> superMethod) {
        return tokenSet.stream()
                .anyMatch(token -> FilenameUtils.wildcardMatch(itemName, token)) || superMethod.test(itemName);
    }
}
