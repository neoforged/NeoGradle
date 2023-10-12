package net.neoforged.gradle.mixin;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.bundling.Jar;

public class MixinProjectPlugin implements Plugin<Project> {

    private Mixin extension;

    @Override
    public void apply(Project project) {
        if (project.getPlugins().findPlugin(CommonProjectPlugin.class) == null) {
            throw new IllegalStateException("The mixin extension requires the common plugin to be applied first.");
        }
        this.extension = project.getExtensions().create(Mixin.class, Mixin.EXTENSION_NAME, MixinExtension.class, project);
        project.afterEvaluate(p -> {
            p.getTasks().withType(Jar.class).all(this::configureJarTask);
            p.getExtensions().<NamedDomainObjectContainer<Run>>configure(
                    RunsConstants.Extensions.RUNS,
                    runs -> runs.all(MixinProjectPlugin.this::configureRun)
            );
        });
    }

    private void configureJarTask(Jar jar) {
        jar.getManifest().getAttributes().computeIfAbsent("MixinConfigs", $ -> String.join(",", this.extension.getConfigs().get()));
    }

    private void configureRun(Run run) {
        final ListProperty<String> programArguments = run.getProgramArguments();
        for (String config : this.extension.getConfigs().get()) {
            programArguments.addAll("--mixin.config", config);
        }
    }
}
