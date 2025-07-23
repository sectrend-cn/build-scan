//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.sectrend.buildscan.utils;

import com.sectrend.buildscan.base.graph.DependencyGraph;
import com.sectrend.buildscan.base.graph.MutableMapDependencyGraph;
import com.sectrend.buildscan.model.Dependency;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class MutableMapDependencyGraphUtil {
    private MutableMapDependencyGraphUtil() {
    }

//    public static void copyDirectDependencies(DependencyGraph destinationGraph, DependencyGraph sourceGraph) {
//        Objects.requireNonNull(sourceGraph);
//        copyDependencies(destinationGraph, sourceGraph, sourceGraph::getDirectDependencies);
//    }
//
//    public static void copyDirectDependenciesToParent(DependencyGraph destinationGraph, Dependency parent, DependencyGraph sourceGraph) {
//        Objects.requireNonNull(sourceGraph);
//        copyDependenciesToParent(destinationGraph, parent, sourceGraph, sourceGraph::getDirectDependencies);
//    }

    public static void copyRootMutableMapDependencies(MutableMapDependencyGraph mutableMapDependencyGraph, DependencyGraph dependencyGraph) {
        Objects.requireNonNull(dependencyGraph);
        copyMutableMapDependencies(mutableMapDependencyGraph, dependencyGraph, dependencyGraph::getRootDependencies);
    }

//    public static void copyRootDependenciesToParent(DependencyGraph destinationGraph, Dependency parent, DependencyGraph sourceGraph) {
//        Objects.requireNonNull(sourceGraph);
//        copyDependenciesToParent(destinationGraph, parent, sourceGraph, sourceGraph::getRootDependencies);
//    }

    public static void copyMutableMapDependencies(MutableMapDependencyGraph mutableMapDependencyGraph, DependencyGraph dependencyGraph, Supplier<Set<Dependency>> dependencies) {
        Set<Dependency> encountered = new HashSet();
        Iterator var4 = ((Set)dependencies.get()).iterator();

        while(var4.hasNext()) {
            Dependency dependency = (Dependency)var4.next();
            mutableMapDependencyGraph.addDirectDependency(dependency);
            copyMutableMapDependencyTreeFromGraph(mutableMapDependencyGraph, dependency, dependencyGraph, encountered);
        }

    }

//    public static void copyDependenciesToParent(DependencyGraph destinationGraph, Dependency parent, DependencyGraph sourceGraph, Supplier<Set<Dependency>> dependencies) {
//        Set<Dependency> encountered = new HashSet();
//        Iterator var5 = ((Set)dependencies.get()).iterator();
//
//        while(var5.hasNext()) {
//            Dependency dependency = (Dependency)var5.next();
//            destinationGraph.addChildWithParent(dependency, parent);
//            copyDependencyTreeFromGraph(destinationGraph, dependency, sourceGraph, encountered);
//        }
//
//    }

    private static void copyMutableMapDependencyTreeFromGraph(MutableMapDependencyGraph mutableMapDependencyGraph, Dependency parentDependency, DependencyGraph dependencyGraph, Set<Dependency> encountered) {
        Dependency dependency;
        for(Iterator var4 = dependencyGraph.getChildrenForParent(parentDependency).iterator(); var4.hasNext(); mutableMapDependencyGraph.addChildWithParent(dependency, parentDependency)) {
            dependency = (Dependency)var4.next();
            if (!encountered.contains(dependency)) {
                encountered.add(dependency);
                copyMutableMapDependencyTreeFromGraph(mutableMapDependencyGraph, dependency, dependencyGraph, encountered);
            }
        }

    }
}
