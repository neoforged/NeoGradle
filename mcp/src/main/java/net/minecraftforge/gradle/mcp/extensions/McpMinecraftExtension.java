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

package net.minecraftforge.gradle.mcp.extensions;

import net.minecraftforge.gradle.common.util.RunConfig;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import org.gradle.util.Configurable;

import java.util.Map;

import javax.inject.Inject;

public abstract class McpMinecraftExtension implements Configurable<McpMinecraftExtension> {

    private final Project project;
    private final FilesWithEntriesExtension accessTransformers;
    private final FilesWithEntriesExtension sideAnnotationStrippers;
    private final MappingsExtension mappings;
    private final NamedDomainObjectContainer<RunConfig> runs;

    @Inject
    public McpMinecraftExtension(final Project project) {
        this.project = project;
        this.accessTransformers = project.getObjects().newInstance(FilesWithEntriesExtension.class, project);
        this.sideAnnotationStrippers = project.getObjects().newInstance(FilesWithEntriesExtension.class, project);
        this.mappings = project.getObjects().newInstance(MappingsExtension.class, project);
        this.runs = project.getObjects().domainObjectContainer(RunConfig.class, name -> new RunConfig(project, name));
    }

    public Project getProject() {
        return project;
    }

    public NamedDomainObjectContainer<RunConfig> getRuns() {
        return runs;
    }

    public MappingsExtension getMappings() {
        return mappings;
    }

    public FilesWithEntriesExtension getAccessTransformers() {
        return this.accessTransformers;
    }

    public FilesWithEntriesExtension getSideAnnotationStrippers() {
        return this.sideAnnotationStrippers;
    }
}
