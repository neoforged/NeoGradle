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

import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.plugins.ide.eclipse.model.SourceFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EclipseRunGenerator extends RunConfigGenerator.XMLConfigurationBuilder
{
    @Override
    @Nonnull
    protected Map<String, Document> createRunConfiguration(@Nonnull final Project project, @Nonnull final RunConfiguration runConfiguration, @Nonnull final DocumentBuilder documentBuilder, List<String> additionalClientArgs) {
        final Map<String, Document> documents = new LinkedHashMap<>();

        final File workingDirectory = runConfiguration.getWorkingDirectory().get().getAsFile();

        // Eclipse requires working directory to exist
        if (!workingDirectory.exists()) {
            workingDirectory.mkdirs();
        }

        // Java run config
        final Document javaDocument = documentBuilder.newDocument();
        {
            final Element rootElement = javaDocument.createElement("launchConfiguration");
            {
                rootElement.setAttribute("type", "org.eclipse.jdt.launching.localJavaApplication");

                elementAttribute(javaDocument, rootElement, "string", "org.eclipse.jdt.launching.PROJECT_ATTR", getEclipseProjectName(project));
                elementAttribute(javaDocument, rootElement, "string", "org.eclipse.jdt.launching.MAIN_TYPE", runConfiguration.getMain());
                elementAttribute(javaDocument, rootElement, "string", "org.eclipse.jdt.launching.VM_ARGUMENTS",
                        getJvmArgs(runConfiguration, additionalClientArgs, updatedTokens));
                elementAttribute(javaDocument, rootElement, "string", "org.eclipse.jdt.launching.PROGRAM_ARGUMENTS",
                        getArgs(runConfiguration, updatedTokens));
                elementAttribute(javaDocument, rootElement, "string", "org.eclipse.jdt.launching.WORKING_DIRECTORY", runConfiguration.getWorkingDirectory());

                final Element envs = javaDocument.createElement("mapAttribute");
                {
                    envs.setAttribute("key", "org.eclipse.debug.core.environmentVariables");

                    runConfiguration.getEnvironmentVariables().forEach((name, value) -> {
                        final Element envEntry = javaDocument.createElement("mapEntry");
                        {
                            envEntry.setAttribute("key", name);
                            envEntry.setAttribute("value", Utils.replace(updatedTokens, value));
                        }
                        envs.appendChild(envEntry);
                    });
                }
                rootElement.appendChild(envs);
            }
            javaDocument.appendChild(rootElement);
        }
        documents.put(runConfiguration.getTaskName() + ".launch", javaDocument);

        return documents;
    }

    static String getEclipseProjectName(@Nonnull final Project project) {
        final EclipseModel eclipse = project.getExtensions().findByType(EclipseModel.class);
        return eclipse == null ? project.getName() : eclipse.getProject().getName();
    }

    static Stream<String> mapModClassesToEclipse(@Nonnull final Project project, @Nonnull final RunConfiguration runConfiguration) {
        final Map<SourceSet, Map<String, String>> sourceSetsToOutputs = new IdentityHashMap<>();

        project.getRootProject().getAllprojects().forEach(proj -> {
            final EclipseModel eclipse = proj.getExtensions().findByType(EclipseModel.class);
            if (eclipse == null) return;
            final EclipseClasspath classpath = eclipse.getClasspath();

            final Map<String, String> outputs = classpath.resolveDependencies().stream()
                    .filter(SourceFolder.class::isInstance)
                    .map(SourceFolder.class::cast)
                    .map(SourceFolder::getOutput)
                    .distinct()
                    .collect(Collectors.toMap(output -> output.split("/")[output.split("/").length - 1], output -> proj.file(output).getAbsolutePath()));

            final JavaPluginExtension javaPlugin = proj.getExtensions().findByType(JavaPluginExtension.class);
            if (javaPlugin != null)
            {
                for (SourceSet sourceSet : javaPlugin.getSourceSets())
                {
                    sourceSetsToOutputs.computeIfAbsent(sourceSet, a -> new HashMap<>()).putAll(outputs);
                }
            }
        });

        if (runConfiguration.getMods().isEmpty())
        {
            return runConfiguration.getAllSources().stream()
                    .map(sourceSet -> sourceSetsToOutputs.getOrDefault(sourceSet, Collections.emptyMap()).get(sourceSet.getName()))
                    .filter(Objects::nonNull)
                    .map(s -> String.join(File.pathSeparator, s, s)); // <resources>:<classes>
        } else
        {
            final SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().getByName(SourceSet.MAIN_SOURCE_SET_NAME);

            return runConfiguration.getMods().stream()
                    .flatMap(modConfig -> {
                        return (modConfig.getSources().isEmpty() ? Stream.of(main) : modConfig.getSources().stream())
                                .map(sourceSet -> sourceSetsToOutputs.getOrDefault(sourceSet, Collections.emptyMap()).get(sourceSet.getName()))
                                .filter(Objects::nonNull)
                                .map(output -> modConfig.getName() + "%%" + output)
                                .map(s -> String.join(File.pathSeparator, s, s)); // <resources>:<classes>
                    });
        }
    }
}
