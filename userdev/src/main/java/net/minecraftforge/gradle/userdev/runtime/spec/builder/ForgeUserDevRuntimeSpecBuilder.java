package net.minecraftforge.gradle.userdev.runtime.spec.builder;

import net.minecraftforge.gradle.userdev.runtime.extension.ForgeUserDevRuntimeExtension;
import net.minecraftforge.gradle.userdev.runtime.spec.ForgeUserDevRuntimeSpecification;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

public final class ForgeUserDevRuntimeSpecBuilder {
    private final Project project;
    private Project configureProject;
    private String namePrefix = "";
    private boolean hasConfiguredForgeVersion;
    private Provider<String> forgeVersionProvider;

    private ForgeUserDevRuntimeSpecBuilder(Project project) {
        this.project = project;
        this.configureProject = project;
    }

    public static ForgeUserDevRuntimeSpecBuilder from(final Project project) {
        final ForgeUserDevRuntimeSpecBuilder builder =  new ForgeUserDevRuntimeSpecBuilder(project);

        configureBuilder(builder);

        return builder;
    }

    private static void configureBuilder(ForgeUserDevRuntimeSpecBuilder builder) {
        final ForgeUserDevRuntimeExtension runtimeExtension = builder.configureProject.getExtensions().getByType(ForgeUserDevRuntimeExtension.class);

        if (!builder.hasConfiguredForgeVersion) {
            builder.forgeVersionProvider = runtimeExtension.getDefaultVersion();
        }
    }

    public ForgeUserDevRuntimeSpecBuilder withName(final String namePrefix) {
        this.namePrefix = namePrefix;
        return this;
    }

    public ForgeUserDevRuntimeSpecBuilder withForgeVersion(final Provider<String> mcpVersion) {
        this.forgeVersionProvider = mcpVersion;
        this.hasConfiguredForgeVersion = true;
        return this;
    }

    public ForgeUserDevRuntimeSpecBuilder withForgeVersion(final String mcpVersion) {
        if (mcpVersion == null) // Additional null check for convenient loading of versions from dependencies.
            return this;

        return withForgeVersion(project.provider(() -> mcpVersion));
    }

    public ForgeUserDevRuntimeSpecBuilder configureFromProject(Project configureProject) {
        this.configureProject = configureProject;

        configureBuilder(this);

        return this;
    }

    public ForgeUserDevRuntimeSpecification build() {
        return new ForgeUserDevRuntimeSpecification(project, configureProject, namePrefix, forgeVersionProvider.get());
    }
}
