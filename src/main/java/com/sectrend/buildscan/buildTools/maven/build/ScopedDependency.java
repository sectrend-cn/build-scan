
package com.sectrend.buildscan.buildTools.maven.build;

import com.sectrend.buildscan.model.Dependency;
import com.sectrend.buildscan.model.ForeignId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * </p>
 *
 * @author yhx
 * @date 2022/6/8 13:57
 */
public class ScopedDependency extends Dependency {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final String scope;

    public ScopedDependency(String name, String version, ForeignId foreignId, String scope) {
        super(name, version, foreignId);
        if (scope == null) {
            this.logger.warn(String.format("The range of component %s:%s:%s is missing, which can produce inaccurate results", new Object[] { foreignId.getGroup(), foreignId.getName(), foreignId.getVersion() }));
            this.scope = "";
        } else {
            this.scope = scope;
        }
    }
}
