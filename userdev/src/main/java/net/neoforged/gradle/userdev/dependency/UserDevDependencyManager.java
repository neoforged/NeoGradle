package net.neoforged.gradle.userdev.dependency;

import net.neoforged.gradle.dsl.common.extensions.dependency.replacement.DependencyReplacement;
import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.userdev.runtime.definition.UserDevRuntimeDefinition;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;

import java.util.Collections;
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
        dependencyReplacer.getReplacementHandlers().create("neoForge", dependencyReplacementHandler -> dependencyReplacementHandler.getReplacer().set(context -> {
            if (isNotAMatchingDependency(context.getDependency())) {
                return Optional.empty();
            }
            
            if (!(context.getDependency() instanceof ExternalModuleDependency externalModuleDependency)) {
                return Optional.empty();
            }

            final UserDevRuntimeDefinition runtimeDefinition = buildForgeUserDevRuntimeFrom(project, externalModuleDependency);
            
            final Configuration additionalDependenciesConfiguration = ConfigurationUtils.temporaryConfiguration(
                    project,
                    "NeoForgeUserDevAdditionalReplacementDependenciesFor" + runtimeDefinition.getSpecification().getIdentifier(),
                    configuration -> {
                        configuration.setDescription("Additional compileDependencies for the NeoForge UserDev replacement for " + runtimeDefinition.getSpecification().getIdentifier());
                        configuration.extendsFrom(runtimeDefinition.getNeoFormRuntimeDefinition().getMinecraftDependenciesConfiguration());
                        configuration.extendsFrom(runtimeDefinition.getAdditionalUserDevDependencies());
                    }
            );
            
            return Optional.of(
                    new UserDevReplacementResult(
                            project,
                            runtimeDefinition.getNeoFormRuntimeDefinition().getSourceJarTask(),
                            runtimeDefinition.getNeoFormRuntimeDefinition().getRawJarTask(),
                            additionalDependenciesConfiguration,
                            Collections.emptySet(),
                            runtimeDefinition
                    ));
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
        if (externalModuleDependency.getArtifacts().isEmpty()) {
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

        return forgeRuntimeExtension.maybeCreateFor(dependency, builder -> {

            final ExternalModuleDependency clone = dependency.copy();
            clone.artifact(artifact -> {
                artifact.setExtension("jar");
                artifact.setClassifier("userdev");
            });

            final Configuration userdevLookup = ConfigurationUtils.temporaryUnhandledConfiguration(project.getConfigurations(), "ResolveRequestedNeoForgeVersion", clone);
            final ResolvedArtifact resolvedArtifact = userdevLookup.getResolvedConfiguration().getFirstLevelModuleDependencies().iterator().next().getModuleArtifacts().iterator().next();

            builder.withForgeVersion(resolvedArtifact.getModuleVersion().getId().getVersion());
            builder.withForgeGroup(resolvedArtifact.getModuleVersion().getId().getGroup());
            builder.withForgeName(resolvedArtifact.getModuleVersion().getId().getName());
            builder.withDistributionType(DistributionType.JOINED);
        });
    }
}
