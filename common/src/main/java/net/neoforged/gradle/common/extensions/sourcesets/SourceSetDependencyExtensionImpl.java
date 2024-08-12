package net.neoforged.gradle.common.extensions.sourcesets;

import net.neoforged.gradle.common.extensions.NeoGradleProblemReporter;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetDependencyExtension;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetInheritanceExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class SourceSetDependencyExtensionImpl implements SourceSetDependencyExtension {

    private final Project project;
    private final SourceSet target;

    @Inject
    public SourceSetDependencyExtensionImpl(SourceSet target) {
        this.project = SourceSetUtils.getProject(target);
        this.target = target;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void on(SourceSet sourceSet) {
        final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

        if (sourceSetProject != project) {
            final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
            throw reporter.throwing(spec -> spec
                    .id("source-set-dependencies", "wrong-project")
                    .contextualLabel("on(SourceSet)")
                    .details("SourceSet '%s' is not from the same project as the current SourceSet '%s', as such it can not depend on it".formatted(sourceSet.getName(), project.getName()))
                    .section("common-dep-sourceset-management-depend")
                    .solution("Ensure that the SourceSet is from the same project as the current SourceSet")
            );
        }

        final SourceSetInheritanceExtension sourceSetInheritanceExtension = target.getExtensions().getByType(SourceSetInheritanceExtension.class);
        sourceSetInheritanceExtension.from(sourceSet);

        project.getDependencies().add(
                target.getImplementationConfigurationName(),
                sourceSet.getOutput()
        );
    }
}
