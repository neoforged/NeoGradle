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

package net.minecraftforge.gradle.common.runtime.naming;

import net.minecraftforge.gradle.common.util.ConfigurableObject;
import net.minecraftforge.gradle.common.util.NamingConstants;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import org.gradle.api.Project;

import javax.inject.Inject;

/**
 * A channel provider for a mappings channel.
 * The providers job is, knowing how to construct taskOutputs that can remap certain jar types,
 * like source, compiled or javadoc jar.
 */
public abstract class NamingChannelProvider extends ConfigurableObject<NamingChannel> implements NamingChannel {

    private final Project project;
    private final String name;

    @Inject
    public NamingChannelProvider(Project project, String name) {
        this.project = project;
        this.name = name;

        getMinecraftVersionExtractor().convention(project.getProviders().provider(() -> data -> data.get(NamingConstants.Version.VERSION)));
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public String getName() {
        return name;
    }

}
