package net.minecraftforge.gradle.common.runtime.extensions;

import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.runtime.CommonRuntimeDefinition;
import net.minecraftforge.gradle.common.runtime.spec.CommonRuntimeSpec;
import net.minecraftforge.gradle.common.runtime.spec.builder.CommonRuntimeSpecBuilder;
import net.minecraftforge.gradle.common.runtime.tasks.IRuntimeTask;
import net.minecraftforge.gradle.common.util.*;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import static net.minecraftforge.gradle.common.util.GameArtifactUtils.doWhenRequired;

public abstract class CommonRuntimeExtension<S extends CommonRuntimeSpec, B extends CommonRuntimeSpecBuilder<S, B>, D extends CommonRuntimeDefinition<S>> {
    protected final Map<String, D> runtimes = Maps.newHashMap();
    private final Project project;

    protected CommonRuntimeExtension(Project project) {
        this.project = project;

        this.getSide().convention(ArtifactSide.JOINED);
    }

    protected static void configureGameArtifactProvidingTaskWithDefaults(CommonRuntimeSpec spec, File runtimeWorkingDirectory, Map<String, File> data, IRuntimeTask mcpRuntimeTask, GameArtifact gameArtifact) {
        mcpRuntimeTask.getArguments().set(Maps.newHashMap());
        configureCommonMcpRuntimeTaskParameters(mcpRuntimeTask, data, String.format("provide%s", StringUtils.capitalize(gameArtifact.name())), spec, runtimeWorkingDirectory);
    }

    protected static void configureCommonMcpRuntimeTaskParameters(IRuntimeTask mcpRuntimeTask, Map<String, File> data, String step, CommonRuntimeSpec spec, File runtimeDirectory) {
        mcpRuntimeTask.getData().set(data);
        mcpRuntimeTask.getStepName().set(step);
        mcpRuntimeTask.getDistribution().set(spec.side());
        mcpRuntimeTask.getMinecraftVersion().set(CacheableMinecraftVersion.from(spec.minecraftVersion()));
        mcpRuntimeTask.getRuntimeDirectory().set(runtimeDirectory);
        mcpRuntimeTask.getRuntimeJavaVersion().convention(spec.configureProject().getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion());
    }

    protected static Map<GameArtifact, TaskProvider<? extends IRuntimeTask>> buildDefaultArtifactProviderTasks(final CommonRuntimeSpec spec, final File minecraftCacheFile, final File runtimeWorkingDirectory, final ArtifactSide side) {
        final EnumMap<GameArtifact, TaskProvider<? extends IRuntimeTask>> result = Maps.newEnumMap(GameArtifact.class);

        doWhenRequired(GameArtifact.LAUNCHER_MANIFEST, side, () -> result.put(GameArtifact.LAUNCHER_MANIFEST, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadManifest", "manifest.json", minecraftCacheFile, ICacheFileSelector.launcherMetadata(), "Provides the Minecraft Launcher Manifest from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.VERSION_MANIFEST, side, () -> result.put(GameArtifact.VERSION_MANIFEST, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadJson", String.format("%s.json", spec.minecraftVersion()), minecraftCacheFile, ICacheFileSelector.forVersionJson(spec.minecraftVersion()), "Provides the Minecraft Launcher Version Metadata from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.CLIENT_JAR, side, () -> result.put(GameArtifact.CLIENT_JAR, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadClient", "client.jar", minecraftCacheFile, ICacheFileSelector.forVersionJar(spec.minecraftVersion(), "client"), "Provides the Minecraft Client Jar from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.SERVER_JAR, side, () -> result.put(GameArtifact.SERVER_JAR, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadServer", "server.jar", minecraftCacheFile, ICacheFileSelector.forVersionJar(spec.minecraftVersion(), "server"), "Provides the Minecraft Server Jar from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.CLIENT_MAPPINGS, side, () -> result.put(GameArtifact.CLIENT_MAPPINGS, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadClientMappings", "client_mappings.txt", minecraftCacheFile, ICacheFileSelector.forVersionMappings(spec.minecraftVersion(), "client"), "Provides the Minecraft Client Mappings from the global Minecraft File Cache")));
        doWhenRequired(GameArtifact.SERVER_MAPPINGS, side, () -> result.put(GameArtifact.SERVER_MAPPINGS, CommonRuntimeUtils.createFileCacheEntryProvidingTask(spec, "downloadServerMappings", "server_mappings.txt", minecraftCacheFile, ICacheFileSelector.forVersionMappings(spec.minecraftVersion(), "server"), "Provides the Minecraft Server Mappings from the global Minecraft File Cache")));

        result.forEach(((artifact, taskProvider) -> taskProvider.configure(task -> configureGameArtifactProvidingTaskWithDefaults(spec, runtimeWorkingDirectory, Collections.emptyMap(), task, artifact))));

        return result;
    }

    public Project getProject() {
        return project;
    }

    public abstract Property<ArtifactSide> getSide();

    public final Provider<Map<String, D>> getRuntimes() {
        return getProject().provider(() -> this.runtimes);
    }

    @NotNull
    public final D maybeCreate(final Action<B> configurator) {
        final S spec = createSpec(configurator);
        return maybeCreate(spec);
    }

    @NotNull
    public final D maybeCreate(final Consumer<B> configurator) {
        return maybeCreate((Action<B>) configurator::accept);
    }

    @NotNull
    public final D maybeCreate(final S spec) {
        if (runtimes.containsKey(spec.name()))
            return runtimes.get(spec.name());

        return create(spec);
    }

    @NotNull
    public final D create(final Action<B> configurator) {
        final S spec = createSpec(configurator);
        return create(spec);
    }

    @NotNull
    public final D create(final Consumer<B> configurator) {
        return maybeCreate((Action<B>) configurator::accept);
    }

    @NotNull
    public final D create(final S spec) {
        if (runtimes.containsKey(spec.name()))
            throw new IllegalArgumentException(String.format("Runtime with name '%s' already exists", spec.name()));

        final D runtime = doCreate(spec);
        runtimes.put(spec.name(), runtime);
        return runtime;
    }

    @NotNull
    protected abstract D doCreate(final S spec);

    @NotNull
    private S createSpec(Action<B> configurator) {
        final B builder = createBuilder();
        configurator.execute(builder);
        return builder.build();
    }

    @NotNull
    public final D getByName(final String name) {
        return this.runtimes.computeIfAbsent(name, (n) -> {
            throw new RuntimeException(String.format("Failed to find runtime with name: %s", n));
        });
    }

    @Nullable
    public final D findByName(final String name) {
        return this.runtimes.get(name);
    }

    protected abstract B createBuilder();

    protected abstract void bakeDefinition(D definition);

    public final void bakeDefinitions() {
        this.runtimes.values().forEach(this::bakeDefinition);
    }
}
