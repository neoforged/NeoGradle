package net.minecraftforge.gradle.userdev.dependency;

import com.google.common.collect.Sets;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.minecraftforge.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacementResult;
import net.minecraftforge.gradle.dsl.common.util.CommonRuntimeUtils;
import net.minecraftforge.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.minecraftforge.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.provider.Provider;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

            final Set<Dependency> additionalDependencies = Sets.newHashSet();
            additionalDependencies.addAll(runtimeDefinition.getMcpRuntimeDefinition().getMinecraftDependenciesConfiguration().getDependencies());
            additionalDependencies.addAll(runtimeDefinition.getAdditionalUserDevDependencies().getDependencies());
            final Configuration additionalDependenciesConfiguration = project.getConfigurations().detachedConfiguration(additionalDependencies.toArray(new Dependency[0]));

            return Optional.of(
                    new DependencyReplacementResult(
                            project,
                            name -> CommonRuntimeUtils.buildTaskName(runtimeDefinition.getMcpRuntimeDefinition(), name),
                            runtimeDefinition.getMcpRuntimeDefinition().getSourceJarTask(),
                            runtimeDefinition.getMcpRuntimeDefinition().getRawJarTask(),
                            additionalDependenciesConfiguration,
                            builder -> builder.setVersion(runtimeDefinition.getSpecification().getForgeVersion()),
                            runtimeDefinition::setReplacedDependency,
                            () -> Sets.newHashSet(runtimeDefinition.getAssetsTaskProvider(), runtimeDefinition.getNativesTaskProvider(), runtimeDefinition.getClientExtraJarProvider(), runtimeDefinition.getDebuggingMappingsTaskProvider()))
            );
        }));
    }

    private boolean isNotAMatchingDependency(final Dependency dependencyToCheck) {
        if (dependencyToCheck instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependencyToCheck;
            return externalModuleDependency.getGroup() == null || !externalModuleDependency.getGroup().equals("net.minecraftforge") || !isSupportedSide(dependencyToCheck) || !hasMatchingArtifact(externalModuleDependency);
        }

        return true;
    }

    private boolean isSupportedSide(final Dependency dependency) {
        return dependency.getName().equals("forge");
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

        return forgeRuntimeExtension.maybeCreate(builder -> {
            final String version = (dependency.getVersion() == null ? forgeRuntimeExtension.getDefaultVersion() : project.provider(dependency::getVersion)).getOrNull();

            builder.withForgeVersion(version);
            builder.withName(String.format("dependencyForge%s", version == null ? "" : version));
        });
    }
}
