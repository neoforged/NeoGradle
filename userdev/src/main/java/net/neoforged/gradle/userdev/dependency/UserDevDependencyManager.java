package net.neoforged.gradle.userdev.dependency;

import com.google.common.collect.Sets;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.provider.Provider;

import java.util.Objects;
import java.util.Optional;

public final class UserDevDependencyManager {
    private static final UserDevDependencyManager INSTANCE = new UserDevDependencyManager();

    public static UserDevDependencyManager getInstance() {
        return INSTANCE;
    }

    private UserDevDependencyManager() {
    }

    public void apply(final Project project) {
        final DependencyReplacement dependencyReplacer = project.getExtensions().getByType(DependencyReplacement.class);
        dependencyReplacer.getReplacementHandlers().create("forge", dependencyReplacementHandler -> dependencyReplacementHandler.getReplacer().set(context -> {
            if (isNotAMatchingDependency(context.getDependency())) {
                return Optional.empty();
            }

            if (!(context.getDependency() instanceof ExternalModuleDependency)) {
                return Optional.empty();
            }

            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) context.getDependency();

            final UserDevRuntimeDefinition runtimeDefinition = buildForgeUserDevRuntimeFrom(project, externalModuleDependency);

            final Configuration additionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(project);
            additionalDependenciesConfiguration.extendsFrom(runtimeDefinition.getNeoFormRuntimeDefinition().getMinecraftDependenciesConfiguration());
            additionalDependenciesConfiguration.extendsFrom(runtimeDefinition.getAdditionalUserDevDependencies());

            return Optional.of(
                    new DependencyReplacementResult(
                            project,
                            name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition.getNeoFormRuntimeDefinition(), name),
                            runtimeDefinition.getNeoFormRuntimeDefinition().getSourceJarTask(),
                            runtimeDefinition.getNeoFormRuntimeDefinition().getRawJarTask(),
                            additionalDependenciesConfiguration,
                            builder -> builder.setVersion(runtimeDefinition.getSpecification().getForgeVersion()),
                            builder -> builder.setVersion(runtimeDefinition.getSpecification().getForgeVersion()),
                            runtimeDefinition::setReplacedDependency,
                            runtimeDefinition::onRepoWritten,
                            () -> Sets.newHashSet(runtimeDefinition.getAssets(), runtimeDefinition.getNatives()))
            );
        }));
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !(externalModuleDependency.getGroup().equals("net.minecraftforge") || externalModuleDependency.getGroup().equals("net.neoforged")) || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("forge") || dependency.getName().equals("neoforge");
    }

    private boolean hasMatchingArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().isEmpty()){
            return true;
        }

        return hasSourcesArtifact(externalModuleDependency);
    }

    private static boolean hasSourcesArtifact(ExternalModuleDependency externalModuleDependency) {
        if (externalModuleDependency.getArtifacts().size() != 1) {
            return false;
        }

        final DependencyArtifact artifact = externalModuleDependency.getArtifacts().iterator().next();
        return Objects.equals(artifact.getClassifier(), "sources") && Objects.equals(artifact.getExtension(), "jar");
    }


    private static UserDevRuntimeDefinition buildForgeUserDevRuntimeFrom(Project project, ExternalModuleDependency dependency) {
        final UserDevRuntimeExtension forgeRuntimeExtension = project.getExtensions().getByType(UserDevRuntimeExtension.class);
        final UserDev userDevExtension = project.getExtensions().getByType(UserDev.class);

        return forgeRuntimeExtension.maybeCreate(builder -> {
            final Provider<String> version = project.provider(dependency::getVersion).orElse(userDevExtension.getDefaultForgeVersion());
            final Provider<String> group = project.provider(dependency::getGroup).orElse(userDevExtension.getDefaultForgeGroup());
            final Provider<String> name = project.provider(dependency::getName).orElse(userDevExtension.getDefaultForgeName());

            builder.withForgeVersion(version);
            builder.withForgeGroup(group);
            builder.withForgeName(name);
            builder.withDistributionType(DistributionType.JOINED);
        });
    }
}
