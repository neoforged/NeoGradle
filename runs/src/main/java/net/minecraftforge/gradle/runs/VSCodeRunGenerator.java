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

package net.minecraftforge.gradle.runs;

import com.google.gson.JsonObject;
import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class VSCodeRunGenerator extends RunConfigGenerator.JsonConfigurationBuilder
{
    @Nonnull
    @Override
    protected JsonObject createRunConfiguration(@Nonnull Project project, @Nonnull RunConfiguration runConfiguration, List<String> additionalClientArgs)
    {
        Map<String, Supplier<String>> updatedTokens = configureTokensLazy(project, runConfiguration, mapModClassesToVSCode(project, runConfiguration));

        JsonObject config = new JsonObject();
        config.addProperty("type", "java");
        config.addProperty("name", runConfiguration.getTaskName());
        config.addProperty("request", "launch");
        config.addProperty("mainClass", runConfiguration.getMain());
        config.addProperty("projectName", EclipseRunGenerator.getEclipseProjectName(project));
        config.addProperty("cwd", replaceRootDirBy(project, runConfiguration.getWorkingDirectory(), "${workspaceFolder}"));
        config.addProperty("vmArgs", getJvmArgs(runConfiguration, additionalClientArgs, updatedTokens));
        config.addProperty("args", getArgs(runConfiguration, updatedTokens));
        JsonObject env = new JsonObject();
        runConfiguration.getEnvironmentVariables().forEach((key, value) -> {
            value = Utils.replace(updatedTokens, value);
            if (key.equals("nativesDirectory"))
                value = replaceRootDirBy(project, value, "${workspaceFolder}");
            env.addProperty(key, value);
        });
        config.add("env", env);
        return config;
    }

    private Stream<String> mapModClassesToVSCode(@Nonnull Project project, @Nonnull RunConfiguration runConfiguration)
    {
        return EclipseRunGenerator.mapModClassesToEclipse(project, runConfiguration)
            .map((value) -> replaceRootDirBy(project, value, "${workspaceFolder}"));
    }
}
