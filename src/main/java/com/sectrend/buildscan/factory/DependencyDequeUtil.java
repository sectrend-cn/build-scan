package com.sectrend.buildscan.factory;

import com.sectrend.buildscan.model.Dependency;

import java.util.Deque;
import java.util.LinkedList;

public class DependencyDequeUtil {

    private final Deque<Dependency> dependencyDeque = new LinkedList<>();

    public void clearDependenciesDepth(int dependencyDepth) throws IllegalStateException {
        if (dependencyDepth > this.dependencyDeque.size())
            throw new IllegalStateException(
                    String.format("The dependency level should be less than or equal to %s, but %s. Treat dependencies as having a level of %s.", new Object[] { Integer.valueOf(this.dependencyDeque.size()), Integer.valueOf(dependencyDepth), Integer.valueOf(this.dependencyDeque.size()) }));
        int depthDelta = this.dependencyDeque.size() - dependencyDepth;
        for (int depth = 0; depth < depthDelta; depth++)
            this.dependencyDeque.pop();
    }

    public void clear() {
        this.dependencyDeque.clear();
    }

    public void add(Dependency dependency) {
        this.dependencyDeque.push(dependency);
    }

    public boolean isEmpty() {
        return this.dependencyDeque.isEmpty();
    }

    public Dependency getLastDependency() {
        return this.dependencyDeque.peek();
    }
}
