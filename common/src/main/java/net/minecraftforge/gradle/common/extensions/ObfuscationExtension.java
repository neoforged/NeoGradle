package net.minecraftforge.gradle.common.extensions;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import net.minecraftforge.gradle.common.runtime.naming.UnapplyMappingsTaskBuildingContext;
import net.minecraftforge.gradle.common.tasks.ITaskWithOutput;
import net.minecraftforge.gradle.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.common.util.IConfigurableObject;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

public abstract class ObfuscationExtension extends GroovyObjectSupport implements IConfigurableObject<ObfuscationExtension> {

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

    public abstract Property<Boolean> getCreateAutomatically();

    @SuppressWarnings({"unchecked", "deprecation"})
    public Object methodMissing(String name, Object args) {
        if (!(args instanceof Closure)) {
            return super.invokeMethod(name, args);
        }

        try {
            TaskProvider<? extends Jar> jarTask = project.getTasks().named(name, Jar.class);
            final String unapplyTaskName = CommonRuntimeUtils.buildTaskName(jarTask, "obfuscate");

            TaskProvider<? extends ITaskWithOutput> obfuscateTAsk;
            try {
                obfuscateTAsk = project.getTasks().named(unapplyTaskName, ITaskWithOutput.class);
            } catch (UnknownTaskException e) {
                obfuscateTAsk = createObfuscateTask(jarTask);
            }

            final Closure<? extends ITaskWithOutput> closure = (Closure<? extends ITaskWithOutput>) args;
            return ConfigureUtil.configureSelf(closure, obfuscateTAsk);
        } catch (UnknownTaskException e) {
            return super.invokeMethod(name, args);
        }
    }

    private TaskProvider<? extends ITaskWithOutput> createObfuscateTask(TaskProvider<? extends Jar> jarTask) {
        jarTasksWithObfuscation.add(jarTask.getName());
        final MappingsExtension mappingsExtension = project.getExtensions().getByType(MappingsExtension.class);

        final UnapplyMappingsTaskBuildingContext context = new UnapplyMappingsTaskBuildingContext(
                project,
                jarTask,
                mappingsExtension.getMappingChannel().get(),
                mappingsExtension.getMappingVersion().get()
        );

        return mappingsExtension.getMappingChannel().get().getUnapplyCompiledMappingsTaskBuilder().get().apply(context);
    }
}
