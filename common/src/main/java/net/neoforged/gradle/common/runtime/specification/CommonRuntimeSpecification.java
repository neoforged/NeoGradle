package net.neoforged.gradle.common.runtime.specification;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.dsl.common.runtime.spec.LegacySpecification;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Defines a runtime specification.
 */
public abstract class CommonRuntimeSpecification implements LegacySpecification {
    @NotNull private final Project project;
    @NotNull private final String name;
    @NotNull private final String version;
    @NotNull private final DistributionType distribution;
    @NotNull private final Multimap<String, TaskTreeAdapter> preTaskTypeAdapters;
    @NotNull private final Multimap<String, TaskTreeAdapter> postTypeAdapters;
    @NotNull private final Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers;
    @NotNull private final CommonRuntimeExtension<?,?,?> runtimeExtension;
    protected CommonRuntimeSpecification(Project project,
                                         @NotNull String name,
                                         @NotNull String version,
                                         @NotNull DistributionType distribution,
                                         Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
                                         Multimap<String, TaskTreeAdapter> postTypeAdapters,
                                         Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
                                         @NotNull Class<? extends CommonRuntimeExtension<?, ?, ?>> runtimeExtensionClass) {
        this.project = project;
        this.name = name;
        this.version = version;
        this.distribution = distribution;
        this.preTaskTypeAdapters = ImmutableMultimap.copyOf(preTaskTypeAdapters);
        this.postTypeAdapters = ImmutableMultimap.copyOf(postTypeAdapters);
        this.taskCustomizers = ImmutableMultimap.copyOf(taskCustomizers);
        this.runtimeExtension = project.getExtensions().getByType(runtimeExtensionClass);
    }

    @Override
    @NotNull
    public Project getProject() {
        return project;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public String getIdentifier() {
        return getName() + StringUtils.capitalize(getDistribution().getName().toLowerCase(Locale.ROOT)) + version;
    }
    
    @Override
    public @NotNull String getVersion() {
        return version;
    }
    
    @NotNull
    public final String getVersionedName() {
        final Map<String, ? extends CommonRuntimeDefinition<?>> runtimes = runtimeExtension.getDefinitions();
        if (runtimes.isEmpty())
            return getIdentifier();

        if (runtimes.values().stream().noneMatch(r -> r.getSpecification().equals(this)))
            return getIdentifier();

        if (runtimes.size() == 1)
            return getName();

        return getIdentifier();
    }

    @Override
    @NotNull
    public DistributionType getDistribution() {
        return distribution;
    }

    @Override
    @NotNull
    public Multimap<String, TaskTreeAdapter> getPreTaskTypeAdapters() {
        return preTaskTypeAdapters;
    }

    @Override
    @NotNull
    public Multimap<String, TaskTreeAdapter> getPostTypeAdapters() {
        return postTypeAdapters;
    }

    @NotNull
    @Override
    public Multimap<String, TaskCustomizer<? extends Task>> getTaskCustomizers() {
        return taskCustomizers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonRuntimeSpecification)) return false;

        CommonRuntimeSpecification that = (CommonRuntimeSpecification) o;

        if (!getProject().equals(that.getProject())) return false;
        if (!getIdentifier().equals(that.getIdentifier())) return false;
        if (getDistribution() != that.getDistribution()) return false;
        if (!getPreTaskTypeAdapters().equals(that.getPreTaskTypeAdapters())) return false;
        if (!getTaskCustomizers().equals(that.getTaskCustomizers())) return false;
        return getPostTypeAdapters().equals(that.getPostTypeAdapters());
    }

    @Override
    public int hashCode() {
        int result = getProject().hashCode();
        result = 31 * result + getIdentifier().hashCode();
        result = 31 * result + getDistribution().hashCode();
        result = 31 * result + getPreTaskTypeAdapters().hashCode();
        result = 31 * result + getPostTypeAdapters().hashCode();
        result = 31 * result + getTaskCustomizers().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CommonSpecification{" +
                "project=" + project +
                ", name='" + name + '\'' +
                ", distribution=" + distribution +
                ", preTaskTypeAdapters=" + preTaskTypeAdapters +
                ", postTypeAdapters=" + postTypeAdapters +
                ", taskCustomizers=" + taskCustomizers +
                '}';
    }

    /**
     * Defines a builder for a specification.
     *
     * @param <S> The type of the specification.
     * @param <B> The self-type of the builder.
     */
    public abstract static class Builder<S extends CommonRuntimeSpecification, B extends Builder<S, B>> implements LegacySpecification.Builder<S, B> {

        protected final Project project;
        protected Provider<DistributionType> distributionType;
        protected boolean hasConfiguredDistributionType = false;
        protected final Multimap<String, TaskTreeAdapter> preTaskAdapters = LinkedListMultimap.create();
        protected final Multimap<String, TaskTreeAdapter> postTaskAdapters = LinkedListMultimap.create();
        protected final Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers = LinkedListMultimap.create();

        /**
         * Creates a new builder.
         *
         * @param project The project which will holds the specification.
         */
        protected Builder(Project project) {
            this.project = project;
            configureBuilder();
        }

        /**
         * The current builder instance.
         *
         * @return The builder instance.
         */
        protected abstract B getThis();

        /**
         * Configures the current builder instance from the project configured.
         */
        protected void configureBuilder() {
        }


        @Override
        @NotNull
        public Project getProject() {
            return project;
        }

        @Override
        @NotNull
        public final B withDistributionType(final Provider<DistributionType> distributionType) {
            this.distributionType = distributionType;
            this.hasConfiguredDistributionType = true;
            return getThis();
        }

        @Override
        @NotNull
        public final B withDistributionType(final DistributionType distributionType) {
            if (distributionType == null) // Additional null check for convenient loading of sides from dependencies.
                return getThis();

            return withDistributionType(project.provider(() -> distributionType));
        }

        @Override
        @NotNull
        public final B withPreTaskAdapter(final String taskTypeName, final TaskTreeAdapter adapter) {
            this.preTaskAdapters.put(taskTypeName, adapter);
            return getThis();
        }

        @Override
        @NotNull
        public final B withPostTaskAdapter(final String taskTypeName, final TaskTreeAdapter adapter) {
            this.postTaskAdapters.put(taskTypeName, adapter);
            return getThis();
        }


        @Override
        @NotNull
        public final <T extends Task> B withTaskCustomizer(final String taskTypeName, Class<T> taskType, Consumer<T> customizer) {
            this.taskCustomizers.put(taskTypeName, new TaskCustomizer<>(taskType, customizer));
            return getThis();
        }

        /**
         * Builds the specification.
         *
         * @return The specification.
         */
        @NotNull
        public abstract S build();
    }
}
