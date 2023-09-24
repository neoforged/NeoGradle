package net.neoforged.gradle.common.runtime.naming;

import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.common.runtime.naming.tasks.ApplyOfficialMappingsToSourceJar;
import net.neoforged.gradle.common.util.MappingUtils;
import net.neoforged.gradle.common.util.StreamUtils;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.common.util.GameArtifact;
import net.neoforged.gradle.util.GradleInternalUtils;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OfficialNamingChannelConfigurator {
    private static final OfficialNamingChannelConfigurator INSTANCE = new OfficialNamingChannelConfigurator();

    public static OfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    private OfficialNamingChannelConfigurator() {
    }

    @SuppressWarnings("unchecked")
    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final Mappings mappingsExtension = minecraftExtension.getMappings();
        final Property<Boolean> hasAcceptedLicenseProperty = project.getObjects().property(Boolean.class);
        mappingsExtension.getExtensions().add(TypeOf.typeOf(Property.class), "acceptMojangEula", hasAcceptedLicenseProperty);
        hasAcceptedLicenseProperty.convention(false);

        minecraftExtension.getNamingChannels().register("official", namingChannelProvider -> {
            namingChannelProvider.getApplySourceMappingsTaskBuilder().set(this::buildApplySourceMappingTask);
            namingChannelProvider.getHasAcceptedLicense().convention(project.provider(() -> ((Property<Boolean>) mappingsExtension.getExtensions().getByName("acceptMojangEula")).get()));
            namingChannelProvider.getLicenseText().set(getLicenseText(project));
        });
        minecraftExtension.getMappings().getChannel().convention(minecraftExtension.getNamingChannels().named("official"));
    }
    
    private @NotNull TaskProvider<? extends Runtime> buildApplySourceMappingTask(@NotNull final TaskBuildingContext context) {
        final String mappingVersion = MappingUtils.getVersionOrMinecraftVersion(context.getMappingVersion());

        final String applyTaskName = context.getTaskNameBuilder().apply("applyOfficialMappings");
        return context.getProject().getTasks().register(applyTaskName, ApplyOfficialMappingsToSourceJar.class, applyOfficialMappingsToSourceJar -> {
            applyOfficialMappingsToSourceJar.setGroup("mappings/official");
            applyOfficialMappingsToSourceJar.setDescription(String.format("Applies the Official mappings for version %s.", mappingVersion));

            applyOfficialMappingsToSourceJar.getClientMappingsFile().set(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS).flatMap(WithOutput::getOutput));
            applyOfficialMappingsToSourceJar.getServerMappingsFile().set(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS).flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.getInput().set(context.getInputTask().flatMap(WithOutput::getOutput));

            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.CLIENT_MAPPINGS));
            applyOfficialMappingsToSourceJar.dependsOn(context.getGameArtifactTask(GameArtifact.SERVER_MAPPINGS));
            applyOfficialMappingsToSourceJar.getStepName().set("applyOfficialMappings");
        });
    }

    private @NotNull Provider<String> getLicenseText(Project project) {
        final MinecraftArtifactCache cacheExtension = project.getExtensions().getByType(MinecraftArtifactCache.class);

        return project.provider(() -> GradleInternalUtils.getExtensions(project.getExtensions())
                .stream()
                .filter(CommonRuntimeExtension.class::isInstance)
                .map(extension -> (CommonRuntimeExtension<?,?,?>) extension)
                .collect(Collectors.toList()))
                .map(runtimeExtensions -> runtimeExtensions.stream().map(runtimeExtension -> runtimeExtension.getRuntimes()
                        .map(runtimes -> runtimes.values().stream().map(runtime -> runtime.getSpecification().getMinecraftVersion()).distinct().collect(Collectors.toList()))
                        .map((Transformer<List<File>, List<String>>) minecraftVersions -> {
                            if (minecraftVersions.isEmpty()) {
                                return Collections.emptyList();
                            }

                            return minecraftVersions.stream().map(version -> cacheExtension.cacheVersionMappings(version, DistributionType.CLIENT)).collect(Collectors.toList());
                        })
                        .map((Transformer<List<String>, List<File>>) mappingFiles -> {
                            if (mappingFiles.isEmpty())
                                return Collections.emptyList();

                            return mappingFiles.stream().map(mappingFile -> {
                                try(final Stream<String> lines = Files.lines(mappingFile.toPath())) {
                                    return StreamUtils.takeWhile(lines, line -> line.startsWith("#"))
                                            .map(l -> l.substring(1).trim())
                                            .collect(Collectors.joining("\n"));
                                } catch (IOException e) {
                                    throw new RuntimeException(String.format("Failed to read the mapping license from: %s", mappingFile.getAbsolutePath()), e);
                                }
                            }).distinct().collect(Collectors.toList());
                        })
                        .map(licenses -> {
                            if (licenses.isEmpty()) {
                                return "No license text found";
                            }

                            return licenses.stream().distinct().collect(Collectors.joining("\n\n"));
                        })
                ).collect(Collectors.toList()))
                .map(licenses -> licenses.stream().map(Provider::get).distinct().collect(Collectors.joining("\n\n")));
    }
}

