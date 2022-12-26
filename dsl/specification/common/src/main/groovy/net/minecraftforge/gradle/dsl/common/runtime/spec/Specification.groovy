package net.minecraftforge.gradle.dsl.common.runtime.spec

import com.google.common.collect.LinkedListMultimap
import com.google.common.collect.Multimap
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter
import net.minecraftforge.gradle.dsl.common.util.DistributionType
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.annotations.NotNull

/**
 * Defines a runtime specification.
 */
abstract class Specification implements Serializable {
    @NotNull private final Project project;
    @NotNull private final String name;
    @NotNull private final DistributionType distributionType;
    @NotNull private final Multimap<String, TaskTreeAdapter> preTaskTypeAdapters;
    @NotNull private final Multimap<String, TaskTreeAdapter> postTypeAdapters;

    Specification(Project project, String name, DistributionType distributionType, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters) {
        this.project = project
        this.name = name
        this.distributionType = distributionType
        this.preTaskTypeAdapters = preTaskTypeAdapters
        this.postTypeAdapters = postTypeAdapters
    }

    /**
     * The project which holds the specification.
     *
     * @return the project.
     */
    Project getProject() {
        return project
    }

    /**
     * The name of the specification.
     * Is unique within the project.
     *
     * @return The name.
     */
    String getName() {
        return name
    }

    /**
     * The artifact distribution type of the specification.
     *
     * @return The distribution type.
     */
    DistributionType getDistributionType() {
        return distributionType
    }

    /**
     * The task tree adapters which are invoked before the step (who's name is used as a key) task is being build.
     * These task tree adapters allow for the modification of the input of the steps task with the given name.
     *
     * @return The pre task tree adapters.
     */
    Multimap<String, TaskTreeAdapter> getPreTaskTypeAdapters() {
        return preTaskTypeAdapters
    }

    /**
     * The task tree adapters which are invoked after the step (who's name is used as a key) task has being build.
     * These task tree adapters allow for the modification of the output of the steps task with the given name.
     *
     * @return THe post task tree adapters.
     */
    Multimap<String, TaskTreeAdapter> getPostTypeAdapters() {
        return postTypeAdapters
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (!(o instanceof Specification)) return false

        Specification that = (Specification) o

        if (configureProject != that.configureProject) return false
        if (name != that.name) return false
        if (postTypeAdapters != that.postTypeAdapters) return false
        if (preTaskTypeAdapters != that.preTaskTypeAdapters) return false
        if (project != that.project) return false
        if (distributionType != that.distributionType) return false

        return true
    }

    int hashCode() {
        int result
        result = project.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + distributionType.hashCode()
        result = 31 * result + preTaskTypeAdapters.hashCode()
        result = 31 * result + postTypeAdapters.hashCode()
        return result
    }

    @Override
    String toString() {
        return "Specification{" +
                "project=" + project +
                ", name='" + name + '\'' +
                ", side=" + distributionType +
                ", preTaskTypeAdapters=" + preTaskTypeAdapters +
                ", postTypeAdapters=" + postTypeAdapters +
                '}';
    }

    abstract class Builder<S extends Specification, B extends Builder<S, B>> {

        protected final Project project;
        protected String namePrefix = "";
        protected Provider<DistributionType> side;
        protected boolean hasConfiguredSide = false;
        protected final Multimap<String, TaskTreeAdapter> preTaskAdapters = LinkedListMultimap.create();
        protected final Multimap<String, TaskTreeAdapter> postTaskAdapters = LinkedListMultimap.create();

        protected Builder(Project project) {
            this.project = project;
            configureBuilder();
        }

        protected abstract B getThis();

        protected void configureBuilder() {
            final CommonRuntimeExtension<?,?,?> runtimeExtension = this.project.getExtensions().getByType(CommonRuntimeExtension.class);

            if (!this.hasConfiguredSide) {
                this.side = runtimeExtension.getSide();
            }
        }

        public final B withName(final String namePrefix) {
            this.namePrefix = namePrefix;
            return getThis();
        }

        public final B withSide(final Provider<DistributionType> side) {
            this.side = side;
            this.hasConfiguredSide = true;
            return getThis();
        }

        public final B withSide(final DistributionType side) {
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

}
