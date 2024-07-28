package net.neoforged.gradle.common.dependency;

import net.neoforged.gradle.common.extensions.JarJarExtension;
import net.neoforged.gradle.common.extensions.NeoGradleProblemReporter;
import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.common.dependency.DependencyManagementObject;
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler;
import net.neoforged.jarjar.metadata.ContainedJarIdentifier;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.ResolvedVariantResult;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class JarJarArtifacts {
    private transient final SetProperty<ResolvedComponentResult> includedRootComponents;
    private transient final SetProperty<ResolvedArtifactResult> includedArtifacts;

    private final Project project;
    private final DependencyFilter dependencyFilter;
    private final DependencyVersionInformationHandler dependencyVersionInformationHandler;


    @Internal
    protected SetProperty<ResolvedComponentResult> getIncludedRootComponents() {
        return includedRootComponents;
    }

    @Internal
    protected SetProperty<ResolvedArtifactResult> getIncludedArtifacts() {
        return includedArtifacts;
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Nested
    public abstract ListProperty<ResolvedJarJarArtifact> getResolvedArtifacts();

    @Nested
    public DependencyFilter getDependencyFilter() {
        return dependencyFilter;
    }

    @Nested
    public DependencyVersionInformationHandler getDependencyVersionInformationHandler() {
        return dependencyVersionInformationHandler;
    }

    public JarJarArtifacts(Project project) {
        this.project = project;
        dependencyFilter = getObjectFactory().newInstance(DefaultDependencyFilter.class);
        dependencyVersionInformationHandler = getObjectFactory().newInstance(DefaultDependencyVersionInformationHandler.class);
        includedRootComponents = getObjectFactory().setProperty(ResolvedComponentResult.class);
        includedArtifacts = getObjectFactory().setProperty(ResolvedArtifactResult.class);

        includedArtifacts.finalizeValueOnRead();
        includedRootComponents.finalizeValueOnRead();
        getResolvedArtifacts().finalizeValueOnRead();

        final DependencyFilter filter = getDependencyFilter();
        final DependencyVersionInformationHandler versionHandler = getDependencyVersionInformationHandler();
        getResolvedArtifacts().set(getIncludedRootComponents().zip(getIncludedArtifacts(), (components, artifacts) -> getIncludedJars(filter, versionHandler, components, artifacts)));
    }

    public void configuration(Configuration jarJarConfiguration) {
        getIncludedArtifacts().addAll(jarJarConfiguration.getIncoming().artifactView(config -> {
            config.attributes(
                    attr -> attr.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE)
            );
        }).getArtifacts().getResolvedArtifacts());
        getIncludedRootComponents().add(jarJarConfiguration.getIncoming().getResolutionResult().getRootComponent());
    }

    public void setConfigurations(Collection<? extends Configuration> configurations) {
        includedRootComponents.empty();
        includedArtifacts.empty();
        for (Configuration configuration : configurations) {
            configuration(configuration);
        }
    }

    private List<ResolvedJarJarArtifact> getIncludedJars(DependencyFilter filter, DependencyVersionInformationHandler versionHandler, Set<ResolvedComponentResult> rootComponents, Set<ResolvedArtifactResult> artifacts) {
        Map<ContainedJarIdentifier, String> versions = new HashMap<>();
        Map<ContainedJarIdentifier, String> versionRanges = new HashMap<>();
        Set<ContainedJarIdentifier> knownIdentifiers = new HashSet<>();

        for (ResolvedComponentResult rootComponent : rootComponents) {
            collectFromComponent(rootComponent, knownIdentifiers, versions, versionRanges);
        }
        List<ResolvedJarJarArtifact> data = new ArrayList<>();
        for (ResolvedArtifactResult result : artifacts) {
            ResolvedVariantResult variant = result.getVariant();

            DependencyManagementObject.ArtifactIdentifier artifactIdentifier = capabilityOrModule(variant);
            if (artifactIdentifier == null) {
                continue;
            }

            if (!filter.isIncluded(artifactIdentifier)) {
                continue;
            }
            ContainedJarIdentifier jarIdentifier = new ContainedJarIdentifier(artifactIdentifier.getGroup(), artifactIdentifier.getName());
            if (!knownIdentifiers.contains(jarIdentifier)) {
                continue;
            }

            String version = versionHandler.getVersion(artifactIdentifier).orElse(versions.get(jarIdentifier));
            if (version == null) {
                version = getVersionFrom(variant);
            }

            String versionRange = versionHandler.getVersionRange(artifactIdentifier).orElse(versionRanges.get(jarIdentifier));
            if (versionRange == null) {
                versionRange = getVersionRangeFrom(variant);
            }
            if (versionRange == null) {
                versionRange = makeOpenRange(variant);
            }

            if (version != null && versionRange != null) {
                data.add(new ResolvedJarJarArtifact(result.getFile(), version, versionRange, jarIdentifier.group(), jarIdentifier.artifact()));
            } else {
                final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);

                throw reporter.throwing(spec ->
                        spec.id("jarjar", "no-version-range")
                                .contextualLabel("Missing version range for " + jarIdentifier.group() + ":" + jarIdentifier.artifact())
                                .solution("Ensure that the version is defined in the dependency management block or that the dependency is included in the JarJar configuration")
                                .details("The version for " + jarIdentifier.group() + ":" + jarIdentifier.artifact() + " could not be determined")
                                .section("common-jar-in-jar-publishing")
                );
            }
        }
        return data.stream()
                .sorted(Comparator.comparing(d -> d.getGroup() + ":" + d.getArtifact()))
                .collect(Collectors.toList());
    }

    private void collectFromComponent(ResolvedComponentResult rootComponent, Set<ContainedJarIdentifier> knownIdentifiers, Map<ContainedJarIdentifier, String> versions, Map<ContainedJarIdentifier, String> versionRanges) {
        for (DependencyResult result : rootComponent.getDependencies()) {
            if (!(result instanceof ResolvedDependencyResult resolvedResult)) {
                continue;
            }
            ComponentSelector requested = resolvedResult.getRequested();
            ResolvedVariantResult originalVariant = resolvedResult.getResolvedVariant();
            ResolvedVariantResult variant = originalVariant;
            // We do this to account for any available-at usage in module metadata -- the actual artifact will only have
            // the module ID of the final target of available-at, but the resolved dependency lets us get the whole
            // hierarchy.
            while (variant.getExternalVariant().isPresent()) {
                variant = variant.getExternalVariant().get();
            }
            DependencyManagementObject.ArtifactIdentifier artifactIdentifier = capabilityOrModule(variant);
            if (artifactIdentifier == null) {
                continue;
            }
            ContainedJarIdentifier jarIdentifier = new ContainedJarIdentifier(artifactIdentifier.getGroup(), artifactIdentifier.getName());
            knownIdentifiers.add(jarIdentifier);

            String versionRange = getVersionRangeFrom(variant);
            if (versionRange == null && requested instanceof ModuleComponentSelector) {
                ModuleComponentSelector requestedModule = (ModuleComponentSelector) requested;
                if (isValidVersionRange(requestedModule.getVersionConstraint().getStrictVersion())) {
                    versionRange = requestedModule.getVersionConstraint().getStrictVersion();
                } else if (isValidVersionRange(requestedModule.getVersionConstraint().getRequiredVersion())) {
                    versionRange = requestedModule.getVersionConstraint().getRequiredVersion();
                } else if (isValidVersionRange(requestedModule.getVersionConstraint().getPreferredVersion())) {
                    versionRange = requestedModule.getVersionConstraint().getPreferredVersion();
                }
                if (isValidVersionRange(requestedModule.getVersion())) {
                    versionRange = requestedModule.getVersion();
                }
            }
            if (versionRange == null) {
                versionRange = makeOpenRange(variant);
            }

            String version = getVersionFrom(variant);
            String originalVersion = getVersionFrom(originalVariant);

            if (!Objects.equals(version, originalVersion)) {
                final NeoGradleProblemReporter reporter = project.getExtensions().getByType(NeoGradleProblemReporter.class);
                throw reporter.throwing(spec ->
                        spec.id("jarjar", "version-mismatch")
                                .contextualLabel("Version mismatch for " + originalVariant.getOwner())
                                .solution("Consider depending on the available-at target directly")
                                .details("Version mismatch for " + originalVariant.getOwner() + ": available-at directs to " +
                                        version + " but original is " + originalVersion + " which jarJar cannot handle well")
                                .section("common-jar-in-jar-publishing-moves-and-collisions")
                );
            }

            if (version != null) {
                versions.put(jarIdentifier, version);
            }
            if (versionRange != null) {
                versionRanges.put(jarIdentifier, versionRange);
            }
        }
    }

    private static @Nullable String getVersionRangeFrom(final ResolvedVariantResult variant) {
        return variant.getAttributes().getAttribute(JarJarExtension.JAR_JAR_RANGE_ATTRIBUTE);
    }

    private static @Nullable DependencyManagementObject.ArtifactIdentifier capabilityOrModule(final ResolvedVariantResult variant) {
        DependencyManagementObject.ArtifactIdentifier moduleIdentifier = null;
        if (variant.getOwner() instanceof ModuleComponentIdentifier) {
            ModuleComponentIdentifier moduleComponentIdentifier = (ModuleComponentIdentifier) variant.getOwner();
            moduleIdentifier = new DependencyManagementObject.ArtifactIdentifier(
                    moduleComponentIdentifier.getGroup(),
                    moduleComponentIdentifier.getModule(),
                    moduleComponentIdentifier.getVersion()
            );
        }

        List<DependencyManagementObject.ArtifactIdentifier> capabilityIdentifiers = variant.getCapabilities().stream()
                .map(capability -> new DependencyManagementObject.ArtifactIdentifier(
                        capability.getGroup(),
                        capability.getName(),
                        capability.getVersion()
                ))
                .collect(Collectors.toList());

        if (moduleIdentifier != null && capabilityIdentifiers.contains(moduleIdentifier)) {
            return moduleIdentifier;
        } else if (capabilityIdentifiers.isEmpty()) {
            return null;
        }
        return capabilityIdentifiers.get(0);
    }

    private static @Nullable String moduleOrCapabilityVersion(final ResolvedVariantResult variant) {
        DependencyManagementObject.@Nullable ArtifactIdentifier identifier = capabilityOrModule(variant);
        if (identifier != null) {
            return identifier.getVersion();
        }
        return null;
    }

    private static @Nullable String makeOpenRange(final ResolvedVariantResult variant) {
        String baseVersion = moduleOrCapabilityVersion(variant);

        if (baseVersion == null) {
            return null;
        }

        return "[" + baseVersion + ",)";
    }

    private static @Nullable String getVersionFrom(final ResolvedVariantResult variant) {
        String version = variant.getAttributes().getAttribute(JarJarExtension.FIXED_JAR_JAR_VERSION_ATTRIBUTE);
        if (version == null) {
            version = moduleOrCapabilityVersion(variant);
        }
        return version;
    }

    private static boolean isValidVersionRange(final @Nullable String range) {
        if (range == null) {
            return false;
        }
        try {
            final VersionRange data = VersionRange.createFromVersionSpec(range);
            return data.hasRestrictions() && data.getRecommendedVersion() == null && !range.contains("+");
        } catch (InvalidVersionSpecificationException e) {
            return false;
        }
    }
}
