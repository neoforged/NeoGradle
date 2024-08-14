package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.extensions.base.WithEnabledProperty;
import net.neoforged.gradle.common.extensions.base.WithPropertyLookup;
import net.neoforged.gradle.dsl.common.extensions.subsystems.*;
import net.neoforged.gradle.dsl.common.extensions.subsystems.tools.RenderDocTools;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import javax.inject.Inject;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static net.neoforged.gradle.dsl.common.util.Constants.*;

public abstract class SubsystemsExtension extends WithPropertyLookup implements ConfigurableDSLElement<Subsystems>, Subsystems {

    private final Conventions conventions;
    private final Parchment parchment;
    private final Tools tools;


    @Inject
    public SubsystemsExtension(Project project) {
        super(project);

        this.conventions = project.getObjects().newInstance(ConventionsExtension.class, project);
        this.parchment = project.getObjects().newInstance(ParchmentExtensions.class, project);
        this.tools = project.getObjects().newInstance(ToolsExtension.class, project);

        configureDecompilerDefaults();
        configureRecompilerDefaults();
        configureParchmentDefaults();
        configureToolsDefaults();
        configureDevLoginDefaults();
        configureRenderDocDefaults();
    }

    private void configureRenderDocDefaults() {
        RenderDoc devLogin = getRenderDoc();
        devLogin.getConfigurationSuffix().convention(
                getStringProperty("renderDoc.configurationSuffix", "RenderDocLocalOnly")
        );
    }

    private void configureDevLoginDefaults() {
        DevLogin devLogin = getDevLogin();
        devLogin.getMainClass().convention(
                getStringProperty("devLogin.mainClass", DEVLOGIN_MAIN_CLASS)
        );
        devLogin.getConfigurationSuffix().convention(
                getStringProperty("devLogin.configurationSuffix", "DevLoginLocalOnly")
        );
    }

    private void configureToolsDefaults() {
        Tools tools = getTools();
        tools.getJST().convention(
                getStringProperty("tools.jst", JST_TOOL_ARTIFACT)
        );
        tools.getDevLogin().convention(
                getStringProperty("tools.devLogin", DEVLOGIN_TOOL_ARTIFACT)
        );

        RenderDocTools renderDocTools = tools.getRenderDoc();
        renderDocTools.getRenderDocPath().convention(
                getDirectoryProperty("tools.renderDoc.path", getProject().getLayout().getBuildDirectory().dir("renderdoc"))
        );
        renderDocTools.getRenderDocVersion().convention(
                getStringProperty("tools.renderDoc.version", "1.33")
        );
        renderDocTools.getRenderNurse().convention(
                getStringProperty("tools.renderDoc.renderNurse", RENDERNURSE_TOOL_ARTIFACT)
        );
    }

    private void configureDecompilerDefaults() {
        Decompiler decompiler = getDecompiler();
        decompiler.getMaxMemory().convention(getStringProperty("decompiler.maxMemory", "4g"));
        decompiler.getMaxThreads().convention(getStringProperty("decompiler.maxThreads", "0").map(Integer::parseUnsignedInt));
        decompiler.getLogLevel().convention(getStringProperty("decompiler.logLevel", "ERROR").map(s -> {
            try {
                return DecompilerLogLevel.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new GradleException("Unknown DecompilerLogLevel: " + s + ". Available options: " + Arrays.toString(DecompilerLogLevel.values()));
            }
        }));
        decompiler.getJvmArgs().convention(getSpaceSeparatedListProperty("decompiler.jvmArgs", Collections.emptyList()));
    }

    private void configureRecompilerDefaults() {
        Recompiler recompiler = getRecompiler();
        recompiler.getArgs().convention(getSpaceSeparatedListProperty("recompiler.args", Collections.emptyList()));
        recompiler.getJvmArgs().convention(getSpaceSeparatedListProperty("recompiler.jvmArgs", Collections.emptyList()));
        recompiler.getMaxMemory().convention(getStringProperty("recompiler.maxMemory", DEFAULT_RECOMPILER_MAX_MEMORY));
        recompiler.getShouldFork().convention(getBooleanProperty("recompiler.shouldFork", true, false));
    }

    private void configureParchmentDefaults() {
        Parchment parchment = getParchment();
        // Add a filtered parchment repository automatically if enabled
        project.afterEvaluate(p -> {
            if (!parchment.getIsEnabled().get() || !parchment.getAddRepository().get()) {
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

    @Override
    public Conventions getConventions() {
        return conventions;
    }

    @Override
    public Parchment getParchment() {
        return parchment;
    }

    @Override
    public Tools getTools() {
        return tools;
    }

    public static abstract class ParchmentExtensions extends WithEnabledProperty implements Parchment {

        @Inject
        public ParchmentExtensions(Project project) {
            super(project, "parchment");

            getParchmentArtifact().convention(
                    getStringLocalProperty("parchmentArtifact", null).orElse(
                            getMinecraftVersion()
                                    .zip(getMappingsVersion(), (minecraftVersion, mappingVersion) -> {
                                        return DEFAULT_PARCHMENT_GROUP
                                                + ":" + DEFAULT_PARCHMENT_ARTIFACT_PREFIX + minecraftVersion
                                                + ":" + mappingVersion
                                                + "@zip";
                                    }).orElse("")
                    )
            );
            getConflictPrefix().convention("p_");
            getMinecraftVersion().convention(
                    getStringLocalProperty("minecraftVersion", null)
            );
            getMappingsVersion().convention(
                    getStringLocalProperty("mappingsVersion", null)
            );
            getAddRepository().convention(
                    getBooleanLocalProperty("addRepository", true)
            );
            getIsEnabled().set(getParchmentArtifact()
                    .map(s -> !s.isEmpty()).orElse(getBooleanLocalProperty("enabled", true))
            );
        }
    }
}
