package net.minecraftforge.gradle.userdev.runtime.spec;

import org.gradle.api.Project;

import java.io.Serializable;
import java.util.Objects;

/**
 * Defines a specification for a ForgeUserDev runtime.
 */
public final class ForgeUserDevRuntimeSpec implements Serializable {
    private static final long serialVersionUID = -4521690551635113967L;
    private final Project project;
    private final Project configureProject;
    private final String name;
    private final String forgeVersion;

    /**
     *
     */
    public ForgeUserDevRuntimeSpec(Project project, Project configureProject, String name, String forgeVersion) {
        this.project = project;
        this.configureProject = configureProject;
        this.name = name;
        this.forgeVersion = forgeVersion;
    }

    public Project project() {
        return project;
    }

    public Project configureProject() {
        return configureProject;
    }

    public String name() {
        return name;
    }

    public String forgeVersion() {
        return forgeVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        final ForgeUserDevRuntimeSpec that = (ForgeUserDevRuntimeSpec) obj;
        return Objects.equals(this.project, that.project) &&
                Objects.equals(this.configureProject, that.configureProject) &&
                Objects.equals(this.name, that.name) &&
                Objects.equals(this.forgeVersion, that.forgeVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, configureProject, name, forgeVersion);
    }

    @Override
    public String toString() {
        return "ForgeUserDevRuntimeSpec[" +
                "project=" + project + ", " +
                "configureProject=" + configureProject + ", " +
                "name=" + name + ", " +
                "forgeVersion=" + forgeVersion + ']';
    }

}
