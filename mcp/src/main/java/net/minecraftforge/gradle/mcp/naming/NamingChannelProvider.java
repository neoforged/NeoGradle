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

package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.common.util.IConfigurableObject;
import net.minecraftforge.gradle.mcp.util.McpRuntimeConstants;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

/**
 * A channel provider for a mappings channel.
 * The providers job is, knowing how to construct tasks that can remap certain jar types,
 * like source, compiled or javadoc jar.
 */
public abstract class NamingChannelProvider implements IConfigurableObject<NamingChannelProvider> {

    private final Project project;
    private final String name;

    @Inject
    public NamingChannelProvider(Project project, String name) {
        this.project = project;
        this.name = name;

        getMinecraftVersionExtractor().convention(project.getProviders().provider(() -> data -> data.get(McpRuntimeConstants.Naming.Version.VERSION)));
    }

    /**
     * The project that this provider belongs to.
     *
     * @return The project.
     */
    public Project getProject() {
        return project;
    }

    /**
     * The name of this provider.
     *
     * @return The name.
     */
    public String getName() {
        return name;
    }

    /**
     * The extractor which can extract the minecraft version from the current mapping channel.
     *
     * @return The extractor.
     */
    public abstract Property<MinecraftVersionFromVersionDataProducer> getMinecraftVersionExtractor();

    /**
     * The builder which can construct a new task provider for a source jar mapping.
     * Every time this is invoked the builder needs to produce a new task.
     * However, the builder is allowed to reuse an old task if the inputs match.
     *
     * @return The builder property.
     */
    public abstract Property<ApplyMappingsToSourceJarTaskBuilder> getApplySourceMappingsTaskBuilder();

}
