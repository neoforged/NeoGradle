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

package net.minecraftforge.gradle.mcp.tasks;

import com.google.common.collect.Iterators;
import net.minecraftforge.gradle.mcp.runtime.McpRuntime;
import net.minecraftforge.gradle.mcp.runtime.tasks.McpRuntimeTask;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

@CacheableTask
public abstract class RunMcp extends DefaultTask {

    public RunMcp() {
        getOutputFile().convention(
                getRuntime().map(runTime -> Iterators.getLast(runTime.tasks().values().iterator()))
                        .map(McpRuntimeTask::getOutputFile).map(RegularFileProperty::get)
        );
    }

    @Input
    @Nested
    public abstract Property<McpRuntime> getRuntime();

    @TaskAction
    public void setupMCP() {
        setDidWork(true);
    }

    @OutputFile
    public abstract RegularFileProperty getOutputFile();
}
