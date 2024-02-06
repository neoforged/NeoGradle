package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Decompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.DecompilerLogLevel;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Parchment;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Recompiler;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_ARTIFACT_PREFIX;
import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_GROUP;
import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_MAVEN_URL;
import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_PARCHMENT_TOOL_ARTIFACT;
import static net.neoforged.gradle.dsl.common.util.Constants.DEFAULT_RECOMPILER_MAX_MEMORY;
import static net.neoforged.gradle.dsl.common.util.Constants.SUBSYSTEM_PROPERTY_PREFIX;

public abstract class SubsystemsExtension implements ConfigurableDSLElement<Subsystems>, Subsystems {

    private final Project project;

    @Inject
    public SubsystemsExtension(Project project) {
        this.project = project;

        configureDecompilerDefaults();
        configureRecompilerDefaults();
        configureParchmentDefaults();
    }

    private void configureDecompilerDefaults() {
        Decompiler decompiler = getDecompiler();
        decompiler.getMaxMemory().convention(getStringProperty("decompiler.maxMemory"));
        decompiler.getMaxThreads().convention(getStringProperty("decompiler.maxThreads").map(Integer::parseUnsignedInt));
        decompiler.getLogLevel().convention(getStringProperty("decompiler.logLevel").map(s -> {
            try {
                return DecompilerLogLevel.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new GradleException("Unknown DecompilerLogLevel: " + s + ". Available options: " + Arrays.toString(DecompilerLogLevel.values()));
            }
        }));
        decompiler.getJvmArgs().convention(getSpaceSeparatedListProperty("decompiler.jvmArgs").orElse(Collections.emptyList()));
    }

    private void configureRecompilerDefaults() {
        Recompiler recompiler = getRecompiler();
        recompiler.getArgs().convention(getSpaceSeparatedListProperty("recompiler.args").orElse(Collections.emptyList()));
        recompiler.getJvmArgs().convention(getSpaceSeparatedListProperty("recompiler.jvmArgs").orElse(Collections.emptyList()));
        recompiler.getMaxMemory().convention(getStringProperty("recompiler.maxMemory").orElse(DEFAULT_RECOMPILER_MAX_MEMORY));
    }

    private void configureParchmentDefaults() {
        Parchment parchment = getParchment();
        parchment.getParchmentArtifact().convention(
                getStringProperty("parchment.parchmentArtifact").orElse(
                        parchment.getMinecraftVersion()
                                .zip(parchment.getMappingsVersion(), (minecraftVersion, mappingVersion) -> {
                                    return DEFAULT_PARCHMENT_GROUP
                                            + ":" + DEFAULT_PARCHMENT_ARTIFACT_PREFIX + minecraftVersion
                                            + ":" + mappingVersion
                                            // We need the checked variant for now since it resolves
                                            // parameters conflicting with local variables by prefixing everything with "p"
                                            + ":checked"
                                            + "@zip";
                                })
                )
        );
        parchment.getMinecraftVersion().convention(
                getStringProperty("parchment.minecraftVersion")
        );
        parchment.getMappingsVersion().convention(
                getStringProperty("parchment.mappingsVersion")
        );
        parchment.getToolArtifact().convention(
                getStringProperty("parchment.toolArtifact").orElse(DEFAULT_PARCHMENT_TOOL_ARTIFACT)
        );
        parchment.getAddRepository().convention(
                getBooleanProperty("parchment.addRepository").orElse(true)
        );
        parchment.getEnabled().convention(parchment.getParchmentArtifact()
                .map(s -> !s.isEmpty()).orElse(getBooleanProperty("parchment.enabled").orElse(false)));

        // Add a filtered parchment repository automatically if enabled
        project.afterEvaluate(p -> {
            if (!parchment.getEnabled().get() || !parchment.getAddRepository().get()) {
                return;
            }
            MavenArtifactRepository repo = p.getRepositories().maven(m -> {
                m.setName("Parchment Data");
                m.setUrl(URI.create(DEFAULT_PARCHMENT_MAVEN_URL));
                m.mavenContent(mavenContent -> mavenContent.includeGroup(DEFAULT_PARCHMENT_GROUP));
            });
            // Make sure it comes first due to its filtered group, that should speed up resolution
            p.getRepositories().remove(repo);
            p.getRepositories().addFirst(repo);
        });
    }

    private Provider<String> getStringProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName);
    }

    private Provider<Boolean> getBooleanProperty(String propertyName) {
        String fullPropertyName = SUBSYSTEM_PROPERTY_PREFIX + propertyName;
        return this.project.getProviders().gradleProperty(fullPropertyName)
                .map(value -> {
                    try {
                        return Boolean.valueOf(value);
                    } catch (Exception e) {
                        throw new GradleException("Gradle Property " + fullPropertyName + " is not set to a boolean value: '" + value + "'");
                    }
                });
    }

    private Provider<List<String>> getSpaceSeparatedListProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(SUBSYSTEM_PROPERTY_PREFIX + propertyName)
                .map(s -> Arrays.asList(s.split("\\s+")));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
