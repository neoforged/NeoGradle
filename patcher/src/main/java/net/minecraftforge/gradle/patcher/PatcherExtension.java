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

package net.minecraftforge.gradle.patcher;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.common.config.UserdevConfigurationSpecV2.DataFunction;
import net.neoforged.gradle.mcp.runs.RunConfiguration;
import net.neoforged.gradle.mcp.extensions.McpMinecraftExtension;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class PatcherExtension extends McpMinecraftExtension {
    public static final String EXTENSION_NAME = "patcher";

    private boolean srgPatches = true;
    private boolean notchObf = false;

    private List<Object> extraExcs, extraMappings;

    @Nullable
    private DataFunction processor;

    public PatcherExtension(final Project project) {
        super(project);

        ImmutableMap.<String, String>builder()
                .put(project.getName() + "_client", "mcp.client.Start")
                .put(project.getName() + "_server", "net.minecraft.server.MinecraftServer")
                .build().forEach((name, main) -> {
            RunConfiguration run = new RunConfiguration(project, name);

            run.setTaskName(name);
            run.setMain(main);

            try {
                run.setWorkingDirectory(project.file("run").getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }

            getRunConfigurations().add(run);
        });
    }

    public abstract Property<Project> getParent();

    public void setParent(final Project project) {
        getParent().set(project);

    }

    public abstract RegularFileProperty getCleanSrc();

    public abstract DirectoryProperty getPatchedSrc();

    public abstract DirectoryProperty getPatches();

    public abstract Property<String> getMcVersion();

    public boolean isSrgPatches() {
        return srgPatches;
    }

    public void setSrgPatches(boolean srgPatches) {
        this.srgPatches = srgPatches;
    }

    public boolean getNotchObf() {
        return this.notchObf;
    }

    public void setNotchObf(boolean value) {
        this.notchObf = value;
    }

    public abstract ConfigurableFileCollection getExcs();

    public void setExtraExcs(List<Object> extraExcs) {
        this.extraExcs = new ArrayList<>(extraExcs);
    }

    public void extraExcs(Object... excs) {
        getExtraExcs().addAll(Arrays.asList(excs)); // TODO: Type check!
    }

    public void extraExc(Object exc) {
        extraExcs(exc); // TODO: Type check!
    }

    public List<Object> getExtraExcs() {
        if (extraExcs == null) {
            extraExcs = new ArrayList<>();
        }

        return extraExcs;
    }

    public void extraMapping(Object mapping) {
        if (mapping instanceof String || mapping instanceof File) {
            getExtraMappings().add(mapping);
        } else {
            throw new IllegalArgumentException("Extra mappings must be a file or a string!");
        }
    }

    public void setExtraMappings(List<Object> extraMappings) {
        this.extraMappings = new ArrayList<>(extraMappings);
    }

    public List<Object> getExtraMappings() {
        if (extraMappings == null) {
            extraMappings = new ArrayList<>();
        }

        return extraMappings;
    }

    @Nullable
    public DataFunction getProcessor() {
        return this.processor;
    }

    public abstract MapProperty<String, File> getProcessorData();

    public void setProcessor(Map<String, Object> map) {
        processor(map);
    }

    @SuppressWarnings("unchecked")
    public void processor(Map<String, Object> map) {
        this.processor = new DataFunction();
        map.forEach((key, value) -> {
            if ("tool".equals(key)) {
                if (!(value instanceof String))
                    throw new IllegalArgumentException("'tool' must be a string");
                this.processor.setVersion((String) value);
            } else if ("args".equals(key)) {
                if (value instanceof String)
                    this.processor.setArgs(Collections.singletonList((String) value));
                else if (value instanceof String[])
                    this.processor.setArgs(Arrays.asList((String[]) value));
                else if (value instanceof Collection)
                    this.processor.setArgs(new ArrayList<>((Collection<String>) value));
                else
                    throw new IllegalArgumentException("'args' must be a String, or array of Strings");
            } else if ("repo".equals(key)) {
                if (!(value instanceof String))
                    throw new IllegalArgumentException("'repo' must be a string");
                this.processor.setRepo((String) value);
            } else if ("data".equals(key)) {
                if (!(value instanceof Map))
                    throw new IllegalArgumentException("'data' must be a map of string -> file");
                getProcessorData().putAll((Map<String, File>) value);
            } else {
                throw new IllegalArgumentException("Invalid processor key " + key);
            }
        });
    }

    void copyFrom(PatcherExtension other) {
        getMappings().getMappingChannel().set(other.getMappings().getMappingChannel());
        getMappings().getMappingVersion().set(other.getMappings().getMappingVersion());

        getMcVersion().set(other.getMcVersion());
    }
}
