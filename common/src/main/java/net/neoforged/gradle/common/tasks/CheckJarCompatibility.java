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

package net.neoforged.gradle.common.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.dsl.common.util.Constants;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.util.List;

@CacheableTask
public abstract class CheckJarCompatibility extends JarExec {
    public CheckJarCompatibility() {
        getTool().set(Constants.JARCOMPATIBILITYCHECKER);
        getArgs().addAll("--base-jar", "{base_jar}", "--input-jar", "{input_jar}", "--lib", "{lib}", "--base-lib", "{base_lib}", "--concrete-lib", "{concrete_lib}");

        getBinaryMode().convention(false);
    }

    @Override
    protected List<String> filterArgs(List<String> args) {
        List<String> newArgs = replaceArgs(args,
                ImmutableMap.of(
                        "{base_jar}", getBaseJar().get().getAsFile(),
                        "{input_jar}", getInputJar().get().getAsFile()
                ), ImmutableMap.of(
                        "{lib}", getCommonLibraries().getFiles(),
                        "{base_lib}", getBaseLibraries().getFiles(),
                        "{concrete_lib}", getConcreteLibraries().getFiles()
                ));

        if (getBinaryMode().get()) {
            newArgs.add("--binary");
        } else {
            newArgs.add("--api");
        }

        if (getAnnotationCheckMode().isPresent()) {
            newArgs.add("--annotation-check-mode");
            newArgs.add(getAnnotationCheckMode().get());
        }

        return newArgs;
    }

    @Input
    public abstract Property<Boolean> getBinaryMode();

    @Input
    @Optional
    public abstract Property<String> getAnnotationCheckMode();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getBaseJar();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInputJar();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getCommonLibraries();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getBaseLibraries();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getConcreteLibraries();
}
