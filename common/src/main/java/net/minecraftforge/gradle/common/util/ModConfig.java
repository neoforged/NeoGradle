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

package net.minecraftforge.gradle.common.util;

import com.google.common.collect.Lists;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

public abstract class ModConfig extends GroovyObjectSupport implements IConfigurableObject<ModConfig> {
    private final Provider<String> name;

    public ModConfig(final Project project, final String name) {
        this.name = project.provider(() -> name);

        getSourceSets().convention(project.provider(() -> project.getExtensions().getByType(JavaPluginExtension.class))
                .map(javaPluginExtension -> javaPluginExtension.getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME))
                .map(Lists::newArrayList));
    }

    public final Provider<String> getName() {
        return this.name;
    }

    public abstract ListProperty<SourceSet> getSourceSets();
}
