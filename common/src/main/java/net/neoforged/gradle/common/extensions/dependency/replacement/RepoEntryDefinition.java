package net.neoforged.gradle.common.extensions.dependency.replacement;

import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import net.neoforged.gradle.dsl.common.extensions.repository.EntryDefinition;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.ExtensionContainer;

import javax.inject.Inject;

public abstract class RepoEntryDefinition implements EntryDefinition {

    private final Project project;

    private final Dependency source;
    private final Configuration dependencies;
    private final boolean hasSources;

    @Inject
    public RepoEntryDefinition(Project project, Dependency source, Configuration dependencies, boolean hasSources) {
        this.project = project;
        this.source = source;
        this.dependencies = dependencies;
        this.hasSources = hasSources;
    }

    @Override
    public Entry createFrom(Entry.Builder builder) {
         builder.from(source, dependencies);
         if (!hasSources)
             builder.withoutSources();

         return builder.build();
    }

    @Override
    public Project getProject() {
        return project;
    }
}
