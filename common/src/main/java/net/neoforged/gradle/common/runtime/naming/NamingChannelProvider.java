/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.neoforged.gradle.common.runtime.naming;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.runtime.definition.CommonRuntimeDefinition;
import net.neoforged.gradle.common.runtime.definition.IDelegatingRuntimeDefinition;
import net.neoforged.gradle.common.runtime.naming.tasks.GenerateDebuggingMappings;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.dsl.common.runtime.naming.GenerationTaskBuildingContext;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import net.neoforged.gradle.util.IMappingFileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Optional;

/**
 * A channel provider for a mappings channel.
 * The providers job is, knowing how to construct taskOutputs that can remap certain jar types,
 * like source, compiled or javadoc jar.
 */
public abstract class NamingChannelProvider implements NamingChannel, ConfigurableDSLElement<NamingChannel> {

    private final Project project;
    private final String name;

    @Inject
    public NamingChannelProvider(Project project, String name) {
        this.project = project;
        this.name = name;

        getMinecraftVersionExtractor().convention(project.getProviders().provider(() -> data -> data.get(NamingConstants.Version.VERSION)));
        getDeobfuscationGroupSupplier().convention("");
        getGenerateDebuggingMappingsJarTaskBuilder().convention(this::buildGenerateDebuggingMappingsJarTask);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getName() {
        return name;
    }

    private @NotNull TaskProvider<? extends Runtime> buildGenerateDebuggingMappingsJarTask(@NotNull final GenerationTaskBuildingContext context) {
        Optional<CommonRuntimeDefinition<? extends CommonRuntimeSpecification>> runtimeDefinition = context.getRuntimeDefinition()
                .filter(obj -> obj instanceof CommonRuntimeDefinition)
                .map(obj1 -> (CommonRuntimeDefinition<? extends CommonRuntimeSpecification>) obj1);

        if (!runtimeDefinition.isPresent()) {
            //Resolve delegation
            runtimeDefinition = context.getRuntimeDefinition()
                    .filter(IDelegatingRuntimeDefinition.class::isInstance)
                    .map(IDelegatingRuntimeDefinition.class::cast)
                    .map(IDelegatingRuntimeDefinition::getDelegate)
                    .filter(obj -> obj instanceof CommonRuntimeDefinition)
                    .map(obj1 -> (CommonRuntimeDefinition<? extends CommonRuntimeSpecification>) obj1);
        }

        if (!runtimeDefinition.isPresent()) {
            throw new IllegalStateException("The runtime definition is not present.");
        }
        final CommonRuntimeDefinition<? extends CommonRuntimeSpecification> definition = runtimeDefinition.get();

        final String generateTaskName = context.getTaskNameBuilder().apply("generateDebuggingMappingsJar");

        return context.getProject().getTasks().register(generateTaskName, GenerateDebuggingMappings.class, task -> {
            final TaskProvider<? extends WithOutput> runtimeToSourceMappingsTask = definition.getRuntimeToSourceMappingsTaskProvider();

            task.setGroup("mappings/" + context.getNamingChannel().getName());
            task.setDescription("Generates a jar containing the runtime to source mappings for debugging purposes");

            task.getMappingsFile().set(runtimeToSourceMappingsTask.flatMap(WithOutput::getOutput)
                    .map(RegularFile::getAsFile)
                    .map(TransformerUtils.guard(IMappingFileUtils::load))
                    .map(CacheableIMappingFile::new)
            );
            task.dependsOn(runtimeToSourceMappingsTask);
        });
    }
}
