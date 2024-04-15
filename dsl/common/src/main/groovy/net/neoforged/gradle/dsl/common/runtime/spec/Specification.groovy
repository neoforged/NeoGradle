package net.neoforged.gradle.dsl.common.runtime.spec

import com.google.common.collect.Multimap
import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer
import net.neoforged.gradle.dsl.common.util.DistributionType
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.jetbrains.annotations.NotNull

import java.util.function.Consumer

@CompileStatic
interface Specification {
    /**
     * The project which holds the specification.
     *
     * @return the project.
     */
    @NotNull Project getProject();

    /**
     * The name of the specification.
     * Is not unique within the project.
     *
     * @return The name.
     */
    @NotNull String getName();

    /**
     * The name of the specification.
     * Is unique within the project.
     *
     * Generally combines the name with a unique version.
     *
     * @return The identifier.
     */
    @NotNull String getIdentifier();

    /**
     * The versioned name of the specification.
     * This is unique in the project, it is derived from the name or the identifier.
     *
     * If this specification is the only one registered then it will return the normal name, else the identifier.
     * The idea of this method is to improve the readability of the task tree and logs, in the default case that only
     * a single runtime of that type is registered to any given project.
     *
     * @return The versioned name.
     */
    @NotNull String getVersionedName();

    /**
     * The current version of the specification that is used.
     *
     * @return The version of the specification that is used to build the task tree.
     */
    @NotNull String getVersion();

    /**
     * The artifact distribution type of the specification.
     *
     * @return The distribution type.
     */
    @NotNull DistributionType getDistribution();

    /**
     * The task tree adapters which are invoked before the step (who's name is used as a key) task is being build.
     * These task tree adapters allow for the modification of the input of the steps task with the given name.
     *
     * @return The pre task tree adapters.
     */
    @NotNull Multimap<String, TaskTreeAdapter> getPreTaskTypeAdapters();

    /**
     * The task tree adapters which are invoked after the step (who's name is used as a key) task has being build.
     * These task tree adapters allow for the modification of the output of the steps task with the given name.
     *
     * @return THe post task tree adapters.
     */
    @NotNull Multimap<String, TaskTreeAdapter> getPostTypeAdapters();

    /**
     * The customizers that are allowed to change the configuration of Neoform tasks.
     */
    @NotNull Multimap<String, TaskCustomizer<? extends Task>> getTaskCustomizers();

    /**
     * Determines the specifications minecraft version.
     *
     * @return The minecraft version.
     */
    @NotNull
    String getMinecraftVersion()

    /**
     * The usage of the specification.
     *
     * @return The usage.
     */
    @NotNull
    Usage getUsage()

    interface Builder<S extends Specification, B extends Builder<S, B>> {
        /**
         * The project for which a specification is being built.
         *
         * @return The projcet
         */
        @NotNull Project getProject();

        /**
         * Sets the distribution type via a provider of the specification which is about to be build.
         *
         * @param distributionType The distribution type.
         * @return The builder instance.
         */
        B withDistributionType(Provider<DistributionType> distributionType);

        /**
         * Sets the distribution type of the specification which is about to be build.
         *
         * @param distributionType The distribution type.
         * @return The builder instance.
         */
        B withDistributionType(DistributionType distributionType);

        /**
         * Sets the usage of the specification which is about to be build.
         *
         * @param usageProvider The provider of the usage.
         * @return The builder instance.
         */
        B withUsage(Provider<Usage> usageProvider);

        /**
         * Sets the usage of the specification which is about to be build.
         *
         * @param usage The usage.
         * @return The builder instance.
         */
        B withUsage(Usage usage);

        /**
         * Adds a pre task tree adapter to the specification which is about to be build.
         *
         * @param taskTypeName The name of the task type or specification step before which the task tree adapter should be invoked.
         * @param adapter      The task tree adapter.
         * @return The builder instance.
         */
        B withPreTaskAdapter(String taskTypeName, TaskTreeAdapter adapter);

        /**
         * Adds a post task tree adapter to the specification which is about to be build.
         *
         * @param taskTypeName The name of the task type or specification step after which the task tree adapter should be invoked.
         * @param adapter      The task tree adapter.
         * @return The builder instance.
         */
        B withPostTaskAdapter(String taskTypeName, TaskTreeAdapter adapter);

        /**
         * Adds a task customizer to the specification which is about to be build.
         *
         * @param taskTypeName The name of the task type or specification step to which the customization should be applied.
         * @param taskType     The expected Gradle task type of the task.
         * @param customizer   The function to apply to the task via {@link Task#configure}.
         * @return The builder instance.
         */
         <T extends Task> B withTaskCustomizer(final String taskTypeName, Class<T> taskType, Consumer<T> customizer);
    }

    /**
     * Defines the usage of this specification.
     * Some specification types do not care for usage and are always baked and configured.
     */
    enum Usage {
        /**
         * The specification is only used to extract runs and tasks.
         */
        RUN_ONLY,

        /**
         * The specification is used to extract runs and tasks and is also used to configure the project.
         */
        FULL
    }
}
