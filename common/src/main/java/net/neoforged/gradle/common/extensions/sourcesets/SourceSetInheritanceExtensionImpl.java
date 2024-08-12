package net.neoforged.gradle.common.extensions.sourcesets;

import net.neoforged.gradle.common.extensions.NeoGradleProblemReporter;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.extensions.sourceset.SourceSetInheritanceExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class SourceSetInheritanceExtensionImpl implements SourceSetInheritanceExtension {

    private final Project project;
    private final SourceSet target;

    @Inject
    public SourceSetInheritanceExtensionImpl(SourceSet target) {
        this.project = SourceSetUtils.getProject(target);
        this.target = target;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void from(SourceSet sourceSet) {
        final Project sourceSetProject = SourceSetUtils.getProject(sourceSet);

        if (sourceSetProject != project) {
            final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
            throw reporter.throwing(spec -> spec
                    .id("source-set-inheritance", "wrong-project")
                    .contextualLabel("from(SourceSet)")
                    .details("SourceSet '%s' is not from the same project as the current SourceSet '%s', as such it can not inherit from it".formatted(sourceSet.getName(), project.getName()))
                    .section("common-dep-sourceset-management-inherit")
                    .solution("Ensure that the SourceSet is from the same project as the current SourceSet")
            );
        }

        project.getConfigurations().getByName(target.getCompileClasspathConfigurationName()).extendsFrom(
                project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName())
        );
        project.getConfigurations().getByName(target.getRuntimeClasspathConfigurationName()).extendsFrom(
                project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName())
        );
    }
}
