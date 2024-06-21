package net.neoforged.gradle.common.runtime.extensions;

import net.neoforged.gradle.common.util.DelegatingDomainObjectContainer;
import net.neoforged.gradle.dsl.common.runtime.definition.Definition;
import net.neoforged.gradle.dsl.common.runtime.definition.DependencyHandler;
import net.neoforged.gradle.dsl.common.runtime.definition.Outputs;
import net.neoforged.gradle.dsl.common.runtime.extensions.RuntimesContainer;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.spec.TaskTreeBuilder;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public abstract class DefaultRuntimesContainer extends DelegatingDomainObjectContainer<Definition> implements RuntimesContainer {

    @Inject
    public DefaultRuntimesContainer(Project project) {
        super(project, Definition.class, name -> {
            throw new InvalidUserDataException("Cannot create a runtime definition without a specification. Use register(Specification, TaskTreeBuilder) instead.");
        });
    }

    @Override
    public Definition register(Specification specification, TaskTreeBuilder builder) throws InvalidUserDataException {
        final TaskTreeBuilder.BuildResult buildResult = builder.build(specification);
        final Definition definition = new Definition(
                specification,
                new DependencyHandler(buildResult.compileDependencies(), buildResult.runtimeDependencies()),
                buildResult.handler(),
                buildResult.outputs());

        add(definition);

        return definition;
    }
}
