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

package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.util.Utils;

import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.*;

import com.google.common.collect.ImmutableMap;

import java.util.List;

@CacheableTask
public abstract class ApplyBinPatches extends JarExec implements WithOutput {
    public ApplyBinPatches() {
        getTool().set(Utils.BINPATCHER);
        getArgs().addAll("--clean", "{clean}", "--output", "{output}", "--apply", "{patch}");

        getOutput().convention(getProject().getLayout().getBuildDirectory().dir(getName()).map(d -> d.file("output.jar")));
    }

    @Override
    protected List<String> filterArgs(List<String> args) {
        return replaceArgs(args, ImmutableMap.of(
                "{clean}", getClean().get().getAsFile(),
                "{output}", getOutput().get().getAsFile(),
                "{patch}", getPatch().get().getAsFile()), null);
    }

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getClean();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getPatch();

    @OutputFile
    public abstract RegularFileProperty getOutput();
}
