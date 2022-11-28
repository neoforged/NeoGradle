package net.minecraftforge.gradle.common.runtime.naming;

import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class UnapplyMappingsTaskBuildingContext {

    private final @NotNull Project project;
    private final @NotNull TaskProvider<? extends Jar> taskOutputToModify;
    private final @NotNull NamingChannelProvider namingChannelProvider;
    private final @NotNull Map<String, String> mappingVersionData;

    public UnapplyMappingsTaskBuildingContext(@NotNull Project project, @NotNull TaskProvider<? extends Jar> taskOutputToModify, NamingChannelProvider namingChannelProvider, Map<String, String> mappingVersionData) {
        this.project = project;
        this.taskOutputToModify = taskOutputToModify;
        this.namingChannelProvider = namingChannelProvider;
        this.mappingVersionData = mappingVersionData;
    }

    public Project project() {
        return project;
    }

    public TaskProvider<? extends Jar> taskOutputToModify() {
        return taskOutputToModify;
    }

    public NamingChannelProvider namingChannelProvider() {
        return namingChannelProvider;
    }

    public Map<String, String> mappingVersionData() {
        return mappingVersionData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UnapplyMappingsTaskBuildingContext)) return false;

        UnapplyMappingsTaskBuildingContext that = (UnapplyMappingsTaskBuildingContext) o;

        if (!project.equals(that.project)) return false;
        if (!taskOutputToModify.equals(that.taskOutputToModify)) return false;
        if (!namingChannelProvider.equals(that.namingChannelProvider)) return false;
        return mappingVersionData.equals(that.mappingVersionData);
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + taskOutputToModify.hashCode();
        result = 31 * result + namingChannelProvider.hashCode();
        result = 31 * result + mappingVersionData.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "UnapplyMappingsTaskBuildingContext{" +
                "project=" + project +
                ", taskOutputToModify=" + taskOutputToModify +
                ", namingChannelProvider=" + namingChannelProvider +
                ", mappingVersionData=" + mappingVersionData +
                '}';
    }
}
