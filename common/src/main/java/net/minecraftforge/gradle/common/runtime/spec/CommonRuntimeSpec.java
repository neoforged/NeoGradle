package net.minecraftforge.gradle.common.runtime.spec;

import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import org.gradle.api.Project;

import java.io.Serializable;

/**
 * Defines a specification for a runtime.
 */
public abstract class CommonRuntimeSpec implements Serializable {
    private static final long serialVersionUID = -3537760562547500214L;
    private final Project project;
    private final Project configureProject;
    private final String name;
    private final DistributionType side;
    private final Multimap<String, TaskTreeAdapter> preTaskTypeAdapters;
    private final Multimap<String, TaskTreeAdapter> postTypeAdapters;

    public CommonRuntimeSpec(Project project, Project configureProject, String name, DistributionType side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters) {
        this.project = project;
        this.configureProject = configureProject;
        this.name = name;
        this.side = side;
        this.preTaskTypeAdapters = preTaskTypeAdapters;
        this.postTypeAdapters = postTypeAdapters;
    }

    public abstract String getMinecraftVersion();

    public Project getProject() {
        return project;
    }

    public Project getConfigurationProject() {
        return configureProject;
    }

    public String getName() {
        return name;
    }

    public DistributionType getSide() {
        return side;
    }

    public Multimap<String, TaskTreeAdapter> getPreTaskTypeAdapters() {
        return preTaskTypeAdapters;
    }

    public Multimap<String, TaskTreeAdapter> getPostTypeAdapters() {
        return postTypeAdapters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommonRuntimeSpec)) return false;

        CommonRuntimeSpec that = (CommonRuntimeSpec) o;

        if (!project.equals(that.project)) return false;
        if (!configureProject.equals(that.configureProject)) return false;
        if (!name.equals(that.name)) return false;
        if (side != that.side) return false;
        if (!preTaskTypeAdapters.equals(that.preTaskTypeAdapters)) return false;
        return postTypeAdapters.equals(that.postTypeAdapters);
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + configureProject.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + side.hashCode();
        result = 31 * result + preTaskTypeAdapters.hashCode();
        result = 31 * result + postTypeAdapters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "CommonRuntimeSpec{" +
                "project=" + project +
                ", configureProject=" + configureProject +
                ", name='" + name + '\'' +
                ", side=" + side +
                ", preTaskTypeAdapters=" + preTaskTypeAdapters +
                ", postTypeAdapters=" + postTypeAdapters +
                '}';
    }
}
