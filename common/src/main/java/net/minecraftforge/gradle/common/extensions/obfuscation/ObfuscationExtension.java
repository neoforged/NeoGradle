package net.minecraftforge.gradle.common.extensions.obfuscation;

import com.google.common.collect.Maps;
import groovy.lang.Closure;
import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.tasks.ArtifactFromOutput;
import net.minecraftforge.gradle.common.tasks.ObfuscatedDependencyMarker;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.obfuscation.Obfuscation;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public abstract class ObfuscationExtension extends ConfigurableObject<Obfuscation> implements Obfuscation {

    private final Project project;

    private final Set<String> jarTasksWithObfuscation = new HashSet<>();

    @Inject
    public ObfuscationExtension(Project project) {
        this.project = project;

        getCreateAutomatically().convention(true);

        project.afterEvaluate(evaluatedProject -> {
            if (getCreateAutomatically().get()) {
                evaluatedProject.getTasks().withType(Jar.class).all(jarTask -> {
                    if (!jarTasksWithObfuscation.contains(jarTask.getName())) {
                        createObfuscateTask(evaluatedProject.getTasks().named(jarTask.getName(), Jar.class));
                    }
                });
            }
        });
    }


    @Override
    public Project getProject() {
        return project;
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    public Object methodMissing(String name, Object args) {
        if (!(args instanceof Closure)) {
            return super.invokeMethod(name, args);
        }

        try {
            TaskProvider<? extends Jar> jarTask = project.getTasks().named(name, Jar.class);
            final String unapplyTaskName = CommonRuntimeUtils.buildTaskName(jarTask, "obfuscate");

            TaskProvider<? extends WithOutput> obfuscateTAsk;
            try {
                obfuscateTAsk = project.getTasks().named(unapplyTaskName, WithOutput.class);
            } catch (UnknownTaskException e) {
                obfuscateTAsk = createObfuscateTask(jarTask);
            }

            final Closure<? extends WithOutput> closure = (Closure<? extends WithOutput>) args;
            return ConfigureUtil.configureSelf(closure, obfuscateTAsk);
        } catch (UnknownTaskException e) {
            return super.invokeMethod(name, args);
        }
    }

    private TaskProvider<? extends WithOutput> createObfuscateTask(TaskProvider<? extends Jar> jarTask) {
        jarTasksWithObfuscation.add(jarTask.getName());
        final Mappings mappingsExtension = project.getExtensions().getByType(Mappings.class);

        //TODO: Handle mapping version detection!
        final TaskProvider<? extends WithOutput> devArtifactProvider = project.getTasks().register("provideDevelop" + StringUtils.capitalize(jarTask.getName()), ArtifactFromOutput.class, task -> {
            task.dependsOn(jarTask);

            task.getInput().set(jarTask.flatMap(Jar::getArchiveFile));
            task.getOutput().set(project.getLayout().getBuildDirectory().dir("libs").flatMap(directory -> directory.file(jarTask.flatMap(jar -> jar.getArchiveFileName().map(fileName -> fileName.substring(0, fileName.length() - 4) + "-dev.jar")))));
        });

        final TaskBuildingContext context = new TaskBuildingContext(
                project,
                CommonRuntimeUtils.buildTaskName(jarTask, "obfuscate"),
                devArtifactProvider,
                Maps.newHashMap(),
                mappingsExtension.getVersion().get(),
                new HashSet<>()
        );

        final TaskProvider<? extends WithOutput> obfuscator = mappingsExtension.getChannel().get().getUnapplyCompiledMappingsTaskBuilder().get().apply(context);
        obfuscator.configure(task -> task.dependsOn(devArtifactProvider));

        final TaskProvider<? extends WithOutput> markerGenerator = project.getTasks().register(CommonRuntimeUtils.buildTaskName(jarTask, "markObfuscated"), ObfuscatedDependencyMarker.class, task -> {
            task.dependsOn(obfuscator);

            task.getObfuscatedJar().set(obfuscator.flatMap(WithOutput::getOutput));
            task.getOutputFileName().set(obfuscator.flatMap(WithOutput::getOutputFileName));
            task.getOutput().set(project.getLayout().getBuildDirectory().file("libs/" + obfuscator.flatMap(WithOutput::getOutputFileName)));
        });

        obfuscator.configure(task -> task.finalizedBy(markerGenerator));
        jarTask.configure(task -> task.finalizedBy(obfuscator));
        return obfuscator;
    }
}
