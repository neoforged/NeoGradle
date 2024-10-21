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

package net.neoforged.gradle.common.extensions;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.common.runtime.naming.NamingChannelProvider;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class MinecraftExtension implements ConfigurableDSLElement<Minecraft>, Minecraft {

    private final Project project;
    private final AccessTransformers accessTransformers;
    private final InterfaceInjections interfaceInjections;
    private final NamedDomainObjectContainer<NamingChannel> namingChannelProviders;

    @Inject
    public MinecraftExtension(final Project project) {
        this.project = project;
        this.accessTransformers = project.getExtensions().getByType(AccessTransformers.class);
        this.interfaceInjections = project.getExtensions().getByType(InterfaceInjections.class);
        this.namingChannelProviders = project.getObjects().domainObjectContainer(NamingChannel.class, name -> project.getObjects().newInstance(NamingChannelProvider.class, project, name));
        
        final String baseName = project.getName().replace(":", "_");
        this.getModIdentifier().convention(project.provider(() -> {
            if (baseName.startsWith("_"))
                return baseName.substring(1);
            
            return baseName;
        }));
    }

    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    @NotNull
    @Override
    public NamedDomainObjectContainer<NamingChannel> getNamingChannels() {
        return namingChannelProviders;
    }

    @NotNull
    @Override
    public Mappings getMappings() {
        return project.getExtensions().getByType(Mappings.class);
    }

    @NotNull
    @Override
    public AccessTransformers getAccessTransformers() {
        return this.accessTransformers;
    }

    @NotNull
    @Override
    public InterfaceInjections getInterfaceInjections() {
        return interfaceInjections;
    }
}
