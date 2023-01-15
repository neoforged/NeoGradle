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

package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import net.minecraftforge.gradle.base.util.ConfigurableObject;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.Deobfuscation;
import net.minecraftforge.gradle.dsl.common.extensions.Mappings;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public abstract class MinecraftExtension extends ConfigurableObject<Minecraft> implements Minecraft {

    private final Project project;
    private final AccessTransformers accessTransformers;
    private final NamedDomainObjectContainer<NamingChannel> namingChannelProviders;

    @Inject
    public MinecraftExtension(final Project project) {
        this.project = project;
        this.accessTransformers = project.getExtensions().getByType(AccessTransformers.class);
        this.namingChannelProviders = project.getObjects().domainObjectContainer(NamingChannel.class, name -> project.getObjects().newInstance(NamingChannelProvider.class, project, name));
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public NamedDomainObjectContainer<NamingChannel> getNamingChannelProviders() {
        return namingChannelProviders;
    }

    @Override
    public Mappings getMappings() {
        return project.getExtensions().getByType(Mappings.class);
    }

    @Override
    public AccessTransformers getAccessTransformers() {
        return this.accessTransformers;
    }

    @NotNull
    @Override
    public Deobfuscation getDeobfuscation() {
        return project.getExtensions().getByType(DeobfuscationExtension.class);
    }
}
