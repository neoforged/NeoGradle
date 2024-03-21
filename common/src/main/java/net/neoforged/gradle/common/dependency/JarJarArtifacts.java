package net.neoforged.gradle.common.dependency;

import net.neoforged.gradle.common.extensions.JarJarExtension;
import net.neoforged.gradle.dsl.common.dependency.DependencyFilter;
import net.neoforged.gradle.dsl.common.dependency.DependencyManagementObject;
import net.neoforged.gradle.dsl.common.dependency.DependencyVersionInformationHandler;
import net.neoforged.jarjar.metadata.ContainedJarIdentifier;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.*;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

public abstract class JarJarArtifacts {
    private transient final SetProperty<ResolvedComponentResult> includedRootComponents;
    private transient final SetProperty<ResolvedArtifactResult> includedArtifacts;

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

    public JarJarArtifacts() {
        dependencyFilter = getObjectFactory().newInstance(DefaultDependencyFilter.class);
        dependencyVersionInformationHandler = getObjectFactory().newInstance(DefaultDependencyVersionInformationHandler.class);
        includedRootComponents = getObjectFactory().setProperty(ResolvedComponentResult.class);
        includedArtifacts = getObjectFactory().setProperty(ResolvedArtifactResult.class);

        includedArtifacts.finalizeValueOnRead();
        includedRootComponents.finalizeValueOnRead();

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

    private static List<ResolvedJarJarArtifact> getIncludedJars(DependencyFilter filter, DependencyVersionInformationHandler versionHandler, Set<ResolvedComponentResult> rootComponents, Set<ResolvedArtifactResult> artifacts) {
        Map<ContainedJarIdentifier, String> versions = new HashMap<>();
        Map<ContainedJarIdentifier, String> versionRanges = new HashMap<>();
        Set<ContainedJarIdentifier> knownIdentifiers = new HashSet<>();

        for (ResolvedComponentResult rootComponent : rootComponents) {
            collectFromComponent(rootComponent, knownIdentifiers, versions, versionRanges);
        }
        List<ResolvedJarJarArtifact> data = new ArrayList<>();
        for (ResolvedArtifactResult result : artifacts) {
            ResolvedVariantResult variant = result.getVariant();
            if (!(variant.getOwner() instanceof ModuleComponentIdentifier)) {
                continue;
            }
            ModuleComponentIdentifier componentIdentifier = (ModuleComponentIdentifier) variant.getOwner();
            DependencyManagementObject.ArtifactIdentifier artifactIdentifier = new DependencyManagementObject.ArtifactIdentifier(
                    componentIdentifier.getModuleIdentifier().getGroup(),
                    componentIdentifier.getModuleIdentifier().getName(),
                    componentIdentifier.getVersion()
            );
            if (!filter.isIncluded(artifactIdentifier)) {
                continue;
            }
            ModuleIdentifier identifier = componentIdentifier.getModuleIdentifier();
            ContainedJarIdentifier jarIdentifier = new ContainedJarIdentifier(identifier.getGroup(), identifier.getName());
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
            }
        }
        return data.stream()
                .sorted(Comparator.comparing(d -> d.getGroup() + ":" + d.getArtifact()))
                .collect(Collectors.toList());
    }

    private static void collectFromComponent(ResolvedComponentResult rootComponent, Set<ContainedJarIdentifier> knownIdentifiers, Map<ContainedJarIdentifier, String> versions, Map<ContainedJarIdentifier, String> versionRanges) {
        for (DependencyResult result : rootComponent.getDependencies()) {
            if (!(result instanceof ResolvedDependencyResult)) {
                continue;
            }
            ResolvedDependencyResult resolvedResult = (ResolvedDependencyResult) result;
            ComponentSelector requested = resolvedResult.getRequested();
            ResolvedVariantResult variant = resolvedResult.getResolvedVariant();
            if (!(variant.getOwner() instanceof ModuleComponentIdentifier)) {
                continue;
            }
            ModuleComponentIdentifier componentIdentifier = (ModuleComponentIdentifier) variant.getOwner();
            ModuleIdentifier identifier = componentIdentifier.getModuleIdentifier();
            ContainedJarIdentifier jarIdentifier = new ContainedJarIdentifier(identifier.getGroup(), identifier.getName());
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
                } if (isValidVersionRange(requestedModule.getVersion())) {
                    versionRange = requestedModule.getVersion();
                }
            }
            if (versionRange == null) {
                versionRange = makeOpenRange(variant);
            }

            String version = getVersionFrom(variant);

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

    private static @Nullable String makeOpenRange(final ResolvedVariantResult variant) {
        ComponentIdentifier identifier = variant.getOwner();
        if (identifier instanceof ModuleComponentIdentifier) {
            return "[" + ((ModuleComponentIdentifier) identifier).getVersion() + ",)";
        }
        return null;
    }

    private static @Nullable String getVersionFrom(final ResolvedVariantResult variant) {
        ComponentIdentifier identifier = variant.getOwner();
        String version = variant.getAttributes().getAttribute(JarJarExtension.FIXED_JAR_JAR_VERSION_ATTRIBUTE);
        if (version == null && identifier instanceof ModuleComponentIdentifier) {
            version = ((ModuleComponentIdentifier) identifier).getVersion();
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
