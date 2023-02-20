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

import net.minecraftforge.gradle.common.CommonPlugin;
import net.minecraftforge.gradle.dsl.common.util.Constants;
import net.minecraftforge.gradle.dsl.mcp.extensions.Mcp;
import net.minecraftforge.gradle.mcp.dependency.McpDependencyManager;
import net.minecraftforge.gradle.mcp.extensions.McpExtension;
import net.minecraftforge.gradle.mcp.naming.MCPNamingChannelConfigurator;
import net.minecraftforge.gradle.mcp.naming.MCPOfficialNamingChannelConfigurator;
import net.minecraftforge.gradle.mcp.runtime.extensions.McpRuntimeExtension;
import net.minecraftforge.gradle.runs.RunsPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class McpProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // Needed to gain access to the common systems
        project.getPluginManager().apply(CommonPlugin.class);
        project.getPluginManager().apply(RunsPlugin.class);

        Mcp extension = project.getExtensions().create(Mcp.class,"mcp", McpExtension.class, project);
        McpRuntimeExtension runtimeExtension = project.getExtensions().create("mcpRuntime", McpRuntimeExtension.class, project);

        MCPNamingChannelConfigurator.getInstance().configure(project);
        MCPOfficialNamingChannelConfigurator.getInstance().configure(project);

        //Setup handling of the dependencies
        McpDependencyManager.getInstance().apply(project);

        //Add Known repos
        project.getRepositories().maven(e -> {
            e.setUrl(Constants.FORGE_MAVEN);
            e.metadataSources(m -> {
                m.mavenPom();
                m.artifact();
            });
        });
    }
}
