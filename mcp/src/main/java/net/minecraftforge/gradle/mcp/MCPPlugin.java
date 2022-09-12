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

package net.minecraftforge.gradle.mcp;

import net.minecraftforge.gradle.common.util.Artifact;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.mcp.tasks.DownloadMCPConfig;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

import javax.annotation.Nonnull;

public class MCPPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // Needed to gain access to the JavaToolchainService as an extension
        project.getPluginManager().apply(JavaPlugin.class);

        McpExtension extension = project.getExtensions().create("mcp", McpExtension.class, project);
        McpRuntimeExtension runtimeExtension = project.getExtensions().create("mcpRuntime", McpRuntimeExtension.class, project);

        TaskProvider<DownloadMCPConfig> downloadConfig = project.getTasks().register("downloadConfig", DownloadMCPConfig.class);

        downloadConfig.configure(task -> {
            task.getConfig().set(extension.getConfig().map(Artifact::getDescriptor));
            task.getOutput().set(project.getLayout().getBuildDirectory().file("mcp_config.zip"));
        });

        project.afterEvaluate(p -> {
            //Add Known repos
            project.getRepositories().maven(e -> {
                e.setUrl(Utils.MOJANG_MAVEN);
                e.metadataSources(MetadataSources::artifact);
            });
            project.getRepositories().maven(e -> {
                e.setUrl(Utils.FORGE_MAVEN);
                e.metadataSources(m -> {
                    m.gradleMetadata();
                    m.mavenPom();
                    m.artifact();
                });
            });
            project.getRepositories().mavenCentral(); //Needed for MCP Deps

            final String sideToAdd = extension.getSide().get();
            runtimeExtension.setup(spec -> spec.withMcpVersion(extension.getConfig().get().getVersion()).withSide(sideToAdd));
        });
    }
}
