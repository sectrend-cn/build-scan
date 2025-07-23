package com.sectrend.buildscan.buildTools.go.gomod.build.analyze;

import com.sectrend.buildscan.buildTools.go.gomod.build.model.GoGraph;
import com.sectrend.buildscan.utils.NameVersion;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class GoGraphAnalyzer {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public List<GoGraph> analyzeRelationshipsFromGoModGraph(Set<String> goModGraphOutput) {
        List<GoGraph> goGraphList = new LinkedList<>();
        for (String line : goModGraphOutput) {
            GoGraph goGraph = analyzeLine(line);
            if (goGraph == null) {
                continue;
            }
            goGraphList.add(goGraph);
        }

        return goGraphList;
    }

    private GoGraph analyzeLine(String line) {
        String[] splits = StringUtils.isBlank(line) ? null : line.split(" ", 2);
        if (splits == null || splits.length != 2) {
            logger.warn("Graph line format invalid: {}", line);
            return null;
        }
        return new GoGraph(extractNameVersion(splits[0]), extractNameVersion(splits[1]));
    }

    /**
     * 只有可以用@隔开并且隔开后为两部分的字符串，一个是名称一个是版本，才能被成功解析。如果不成功，就视为只有名称没有版本
     *
     * @param dependency
     * @return
     */
    private NameVersion extractNameVersion(String dependency) {
        return Optional.of(dependency)
                .filter(d -> d.contains("@"))
                .map(d -> d.split("@", 2))
                .filter(splits -> splits.length == 2)
                .map(splits -> new NameVersion(splits[0], splits[1]))
                .orElse(new NameVersion(dependency, null));
    }
}
