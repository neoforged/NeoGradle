package net.neoforged.gradle.platform;

import net.neoforged.gradle.platform.extensions.DynamicProjectManagementExtension;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PlatformSettingsPlugin implements Plugin<Settings> {

    @Override
    public void apply(@NotNull Settings target) {
        target.getExtensions().create("dynamicProjects", DynamicProjectManagementExtension.class, target);

        target.getPlugins().apply("org.gradle.toolchains.foojay-resolver-convention");

        target.getGradle().beforeProject(new DynamicProjectPluginAdapter(target));

        target.pluginManagement(spec -> {
            spec.repositories(repositories -> {
                repositories.gradlePluginPortal();
                repositories.maven(mavenConfig -> {
                    mavenConfig.setUrl("https://maven.neoforged.net/releases");
                    mavenConfig.setName("NeoForged");
                });
                repositories.mavenLocal();
            });
        });
    }

    private static final class DynamicProjectPluginAdapter implements Action<Project> {

        private final Settings settings;

        private DynamicProjectPluginAdapter(Settings settings) {
            this.settings = settings;
        }

        @Override
        public void execute(@NotNull Project project) {
            final DynamicProjectManagementExtension projectManagementExtension = settings.getExtensions().getByType(DynamicProjectManagementExtension.class);

            final Optional<ProjectDescriptor> match = projectManagementExtension.getDynamicProjects().
                    stream()
                    .filter(desc -> desc.getName().equals(project.getName()))
                    .findFirst();

            match.ifPresent(desc -> project.getPlugins().apply(PlatformPlugin.class));
        }
    }
}
