package net.neoforged.gradle.mixin;

import net.neoforged.gradle.common.CommonProjectPlugin;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import net.neoforged.gradle.dsl.mixin.extension.Mixin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.bundling.Jar;

import java.util.HashSet;
import java.util.Set;

public class MixinProjectPlugin implements Plugin<Project> {

    private Mixin extension;

    @Override
    public void apply(Project project) {
        if (project.getPlugins().findPlugin(CommonProjectPlugin.class) == null) {
            throw new IllegalStateException("The mixin extension requires the common plugin to be applied first.");
        }
        this.extension = project.getExtensions().create(Mixin.class, Mixin.EXTENSION_NAME, MixinExtension.class, project);
        
        project.getTasks().withType(Jar.class).configureEach(this::setupJarTask);
        
        project.getExtensions().configure(
                RunManager.class,
                runs -> runs.configureAll(MixinProjectPlugin.this::setupRun)
        );
        
        project.afterEvaluate(p -> {
            p.getTasks().withType(Jar.class).all(this::configureJarTask);
            p.getExtensions().configure(
                    RunManager.class,
                    runs -> runs.configureAll(MixinProjectPlugin.this::configureRun)
            );
        });
    }
    
    private void setupJarTask(Jar jarTask) {
        jarTask.getExtensions().create(Mixin.EXTENSION_NAME, MixinExtension.class, jarTask.getProject());
    }

    private void configureJarTask(Jar jar) {
        jar.getManifest().getAttributes().computeIfAbsent("MixinConfigs", $ -> {
            final Set<String> configs = new HashSet<>(this.extension.getConfigs().get());
            configs.addAll(jar.getExtensions().getByType(MixinExtension.class).getConfigs().get());
            
            return String.join(",", configs);
        });
    }
    
    private void setupRun(Run run) {
        run.getExtensions().create(Mixin.EXTENSION_NAME, MixinExtension.class, run.getProject());
    }
    
    private void configureRun(Run run) {
        final ListProperty<String> programArguments = run.getArguments();
        
        for (String config : this.extension.getConfigs().get()) {
            programArguments.addAll("--fml.mixin", config);
        }
        
        final MixinExtension runExtension = run.getExtensions().getByType(MixinExtension.class);
        for(String config : runExtension.getConfigs().get()) {
            programArguments.addAll("--fml.mixin", config);
        }
    }
}
