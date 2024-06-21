package net.neoforged.gradle.dsl.common.runtime;

import net.neoforged.gradle.dsl.common.runtime.extensions.RuntimesContainer;
import net.neoforged.gradle.dsl.common.runtime.spec.Specification;
import net.neoforged.gradle.dsl.common.runtime.spec.TaskTreeBuilder;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import org.apache.commons.lang3.StringUtils;

public class CommonRuntimeBuilder {

    protected final Specification specification;

    public CommonRuntimeBuilder(Specification specification) {
        this.specification = specification;
    }

    public TaskTreeBuilder.BuildResult build() {
        throw new IllegalStateException("Not implemented");
    }

    /**
     * Creates a unique task name for a task in the runtime.
     *
     * @param nonePrefixedName The name of the task without the prefix.
     * @return The unique task name.
     * @implNote Will return a simpler name if at the time of calling there is only one runtime with the same identifier.
     */
    protected String createTaskName(String nonePrefixedName) {
        //In case we have multiple runtimes with the same identifier, we need to use the name instead
        //The name holds the full version and distribution type, so it should be unique
        final RuntimesContainer runtimesContainer = specification.project().getExtensions().getByType(RuntimesContainer.class);
        if (runtimesContainer.matching(definition ->
                definition.specification().identifier().equals(specification.identifier()) &&
                        definition.specification() != specification
        ).size() > 1) {
            return specification.name() + StringUtils.capitalize(nonePrefixedName);
        }

        return specification.identifier() + StringUtils.capitalize(nonePrefixedName);
    }
}
