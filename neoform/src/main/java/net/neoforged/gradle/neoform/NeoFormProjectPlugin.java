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

package net.neoforged.gradle.neoform;

import net.neoforged.gradle.common.CommonPlugin;
import net.neoforged.gradle.dsl.common.extensions.repository.Repository;
import net.neoforged.gradle.neoform.dependency.NeoFormDependencyManager;
import net.neoforged.gradle.neoform.naming.NeoFormOfficialNamingChannelConfigurator;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.util.UrlConstants;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import javax.annotation.Nonnull;

public class NeoFormProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // Needed to gain access to the common systems
        project.getPluginManager().apply(CommonPlugin.class);



        NeoFormRuntimeExtension runtimeExtension = project.getExtensions().create("neoFormRuntime", NeoFormRuntimeExtension.class, project);

        NeoFormOfficialNamingChannelConfigurator.getInstance().configure(project);

        //Setup handling of the dependencies
        NeoFormDependencyManager.getInstance().apply(project);

        //Add Known repos
        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.NEO_FORGE_MAVEN);
            e.metadataSources(m -> {
                m.mavenPom();
                m.artifact();
            });
        });

        project.getRepositories().maven(e -> {
            e.setUrl(UrlConstants.MCF_FORGE_MAVEN);
            e.metadataSources(m -> {
                m.mavenPom();
                m.artifact();
            });
        });
    }
}
