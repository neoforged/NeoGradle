package net.neoforged.gradle.common.util;

import org.gradle.api.artifacts.dsl.DependencyCollector;

import javax.inject.Inject;

public interface DependencyCollectorInjector
{

    @Inject
    public DependencyCollector getDependencyCollector();
}
