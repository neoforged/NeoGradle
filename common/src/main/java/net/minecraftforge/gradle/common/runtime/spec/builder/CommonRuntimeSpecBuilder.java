package net.minecraftforge.gradle.common.runtime.spec.builder;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.spec.TaskTreeAdapter;
import net.minecraftforge.gradle.common.util.ArtifactSide;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

@SuppressWarnings("UnusedReturnValue")
public abstract class CommonRuntimeSpecBuilder<S extends CommonRuntimeSpec, B extends CommonRuntimeSpecBuilder<S, B>> {

    protected final Project project;
    protected Project configureProject;
    protected String namePrefix = "";
    protected Provider<ArtifactSide> side;
    protected boolean hasConfiguredSide = false;
    protected final Multimap<String, TaskTreeAdapter> preTaskAdapters = LinkedListMultimap.create();
    protected final Multimap<String, TaskTreeAdapter> postTaskAdapters = LinkedListMultimap.create();

    protected CommonRuntimeSpecBuilder(Project project) {
        this.project = project;
        this.configureProject = project;
        configureBuilder();
    }

    protected abstract B getThis();

    protected void configureBuilder() {
        final CommonRuntimeExtension<?,?,?> runtimeExtension = this.configureProject.getExtensions().getByType(CommonRuntimeExtension.class);

        if (!this.hasConfiguredSide) {
            this.side = runtimeExtension.getSide();
        }
    }

    public final B withName(final String namePrefix) {
        this.namePrefix = namePrefix;
        return getThis();
    }

    public final B withSide(final Provider<ArtifactSide> side) {
        this.side = side;
        this.hasConfiguredSide = true;
        return getThis();
    }

    public final B withSide(final ArtifactSide side) {
        if (side == null) // Additional null check for convenient loading of sides from dependencies.
            return getThis();

        return withSide(project.provider(() -> side));
    }

    public final B withPreTaskAdapter(final String taskTypeName, final TaskTreeAdapter adapter) {
        this.preTaskAdapters.put(taskTypeName, adapter);
        return getThis();
    }

    public final B withPostTaskAdapter(final String taskTypeName, final TaskTreeAdapter adapter) {
        this.postTaskAdapters.put(taskTypeName, adapter);
        return getThis();
    }

    public final B configureFromProject(Project configureProject) {
        this.configureProject = configureProject;

        configureBuilder();

        return getThis();
    }

    public abstract S build();
}
