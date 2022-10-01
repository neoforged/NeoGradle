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

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraftforge.gradle.common.util.ProviderUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.tasks.PrepareRunTask;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class RunConfigGenerator {
    public static void createIDEGenRunsTasks(@Nonnull final McpMinecraftExtension minecraft, @Nonnull final TaskProvider<Task> prepareRuns, @Nonnull final TaskProvider<Task> makeSourceDirs, List<String> additionalClientArgs) {
        final Project project = minecraft.getProject();

        final Map<String, Triple<List<Object>, File, Supplier<RunConfigGenerator>>> ideConfigurationGenerators = ImmutableMap.<String, Triple<List<Object>, File, Supplier<RunConfigGenerator>>>builder()
                .put("genIntellijRuns", ImmutableTriple.of(Collections.singletonList(prepareRuns),
                        new File(project.getRootProject().getRootDir(), ".idea/runConfigurations"),
                        () -> new IntellijRunGenerator(project.getRootProject())))
                .put("genEclipseRuns", ImmutableTriple.of(ImmutableList.of(prepareRuns, makeSourceDirs),
                        project.getProjectDir(),
                        EclipseRunGenerator::new))
                .put("genVSCodeRuns", ImmutableTriple.of(ImmutableList.of(prepareRuns, makeSourceDirs),
                        new File(project.getProjectDir(), ".vscode"),
                        VSCodeRunGenerator::new))
                .build();

        ideConfigurationGenerators.forEach((taskName, configurationGenerator) -> {
            project.getTasks().register(taskName, Task.class, task -> {
                task.setGroup(RunConfiguration.RUNS_GROUP);
                task.dependsOn(configurationGenerator.getLeft());

                task.doLast(t -> {
                    final File runConfigurationsDir = configurationGenerator.getMiddle();

                    if (!runConfigurationsDir.exists()) {
                        runConfigurationsDir.mkdirs();
                    }
                    configurationGenerator.getRight().get().createRunConfiguration(minecraft, runConfigurationsDir, project, additionalClientArgs);
                });
            });
        });
    }

    protected static void elementOption(@Nonnull Document document, @Nonnull final Element parent, @Nonnull final String name, @Nonnull final String value) {
        final Element option = document.createElement("option");
        {
            option.setAttribute("name", name);
            option.setAttribute("value", value);
        }
        parent.appendChild(option);
    }

    protected static void elementAttribute(@Nonnull Document document, @Nonnull final Element parent, @Nonnull final String attributeType, @Nonnull final String key, @Nonnull final String value) {
        final Element attribute = document.createElement(attributeType + "Attribute");
        {
            attribute.setAttribute("key", key);
            attribute.setAttribute("value", value);
        }
        parent.appendChild(attribute);
    }

    protected static String replaceRootDirBy(@Nonnull final Project project, String value, @Nonnull final String replacement) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace(project.getRootDir().toString(), replacement);
    }

    protected static Provider<Collection<String>> mapModClassesToGradle(Project project, RunConfiguration runConfiguration) {
        record ModWithSources(Provider<String> name, File classPath, Optional<File> resourcePath) {
            public String toPathString() {
                return name.get() + "%%" + classPath.getAbsolutePath() + (resourcePath.map(file -> File.pathSeparator + file.getAbsolutePath()).orElse(""));
            }
        }

        final Provider<Collection<Provider<List<ModWithSources>>>> sourceSets = ProviderUtils.getNamedCollectionEntriesAsProvider(project, runConfiguration.getMods())
                .map(modConfigs -> modConfigs.stream()
                        .map(modConfig -> modConfig.getSourceSets()
                                .map(sets -> sets.stream()
                                        .map(set -> new ModWithSources(
                                                modConfig.getName(),
                                                set.getOutput().getClassesDirs().getSingleFile().getAbsoluteFile(),
                                                Optional.ofNullable(set.getOutput().getResourcesDir())))
                                        .collect(Collectors.toList())))
                        .collect(Collectors.toSet()));

        return sourceSets.flatMap(providers -> providers.stream().reduce(ProviderUtils::reduceListProviders)
                        .orElseGet(() -> project.provider(Collections::emptyList)))
                .map(listWithSources -> listWithSources.stream().map(ModWithSources::toPathString).collect(Collectors.toList()));
    }

    protected static Map<String, Supplier<String>> configureTokensLazy(final Project project, @Nonnull RunConfiguration runConfiguration, Stream<String> modClasses) {
        Map<String, Supplier<String>> tokens = new HashMap<>();
        runConfiguration.getTokens().forEach((k, v) -> tokens.put(k, () -> v));
        runConfiguration.getLazyTokens().forEach((k, v) -> tokens.put(k, Suppliers.memoize(v::get)));
        tokens.compute("source_roots", (key, sourceRoots) -> Suppliers.memoize(() -> ((sourceRoots != null)
                ? Stream.concat(Arrays.stream(sourceRoots.get().split(File.pathSeparator)), modClasses)
                : modClasses).distinct().collect(Collectors.joining(File.pathSeparator))));
        BiFunction<Supplier<String>, String, String> classpathJoiner = (supplier, evaluated) -> {
            if (supplier == null)
                return evaluated;
            String oldCp = supplier.get();
            return oldCp == null || oldCp.isEmpty() ? evaluated : String.join(File.pathSeparator, oldCp, evaluated);
        };
        // Can't lazily evaluate these as they create tasks we have to do in the current context
        String runtimeClasspath = classpathJoiner.apply(tokens.get("runtime_classpath"), createRuntimeClassPathList(project));
        tokens.put("runtime_classpath", () -> runtimeClasspath);
        String minecraftClasspath = classpathJoiner.apply(tokens.get("minecraft_classpath"), createMinecraftClassPath(project));
        tokens.put("minecraft_classpath", () -> minecraftClasspath);

        File classpathFolder = new File(project.getBuildDir(), "classpath");
        BinaryOperator<String> classpathFileWriter = (filename, classpath) -> {
            if (!classpathFolder.isDirectory() && !classpathFolder.mkdirs())
                throw new IllegalStateException("Could not create directory at " + classpathFolder.getAbsolutePath());
            File outputFile = new File(classpathFolder, runConfiguration.getUniqueFileName() + "_" + filename + ".txt");
            try (Writer classpathWriter = Files.newBufferedWriter(outputFile.toPath(), StandardCharsets.UTF_8)) {
                IOUtils.write(String.join(System.lineSeparator(), classpath.split(File.pathSeparator)), classpathWriter);
            } catch (IOException e) {
                project.getLogger().error("Exception when writing classpath to file {}", outputFile, e);
            }
            return outputFile.getAbsolutePath();
        };
        tokens.put("runtime_classpath_file",
                Suppliers.memoize(() -> classpathFileWriter.apply("runtimeClasspath", runtimeClasspath)));
        tokens.put("minecraft_classpath_file",
                Suppliers.memoize(() -> classpathFileWriter.apply("minecraftClasspath", minecraftClasspath)));

        // *Grumbles about having to keep a workaround for a "dummy" hack that should have never existed*
        runConfiguration.getEnvironmentVariables().compute("MOD_CLASSES", (key, value) ->
                Strings.isNullOrEmpty(value) || "dummy".equals(value) ? "{source_roots}" : value);

        return tokens;
    }

    private static String getResolvedClasspath(Configuration toResolve) {
        return toResolve.copyRecursive().resolve().stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.joining(File.pathSeparator));
    }

    protected static String createRuntimeClassPathList(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration runtimeClasspath = configurations.getByName("runtimeClasspath");
        return getResolvedClasspath(runtimeClasspath);
    }

    protected static String createMinecraftClassPath(final Project project) {
        ConfigurationContainer configurations = project.getConfigurations();
        Configuration minecraft = configurations.findByName("minecraft");
        if (minecraft == null)
            minecraft = configurations.findByName("minecraftImplementation");
        if (minecraft == null)
            throw new IllegalStateException("Could not find valid minecraft configuration!");
        return getResolvedClasspath(minecraft);
    }

    public static void createRunTask(final RunConfiguration runConfiguration, final Project project, final TaskProvider<? extends Task> prepareRuns, final List<String> additionalClientArgs) {

        Map<String, Supplier<String>> updatedTokens = configureTokensLazy(project, runConfiguration, mapModClassesToGradle(project, runConfiguration));

        project.getTasks().addLater(runConfiguration.getTaskName().map(taskName -> project.getTasks().create("prepare" + Utils.capitalize(taskName), PrepareRunTask.class, task -> {
            task.setGroup(RunConfiguration.RUNS_GROUP);

            task.dependsOn(prepareRuns);
            task.dependsOn(runConfiguration.getDependentTasks());

            task.getIsClientRun().set(runConfiguration.getIsClient());
            task.getAdditionalClientArguments().set(additionalClientArgs);
            task.getProgramArguments().set(runConfiguration.getProgramArguments());
            task.getJvmArguments().set(runConfiguration.getJvmArguments());
            task.getEnvironmentVariables().set(runConfiguration.getEnvironmentVariables());
            task.getSourceSets().set(runConfiguration.getAllSourceSets());
            task.getWorkingDirectory().set(runConfiguration.getWorkingDirectory());
        })));

        project.getTasks().addLater(runConfiguration.getTaskName().map(taskName -> project.getTasks().create(taskName, JavaExec.class, task -> {
            final TaskProvider<PrepareRunTask> prepareRunTask = project.getTasks().named("prepare" + Utils.capitalize(taskName), PrepareRunTask.class);

            prepareRunTask.configure(preparingRunTask -> preparingRunTask.getExecutionTask().set(task));

            task.setGroup(RunConfiguration.RUNS_GROUP);
            task.dependsOn(prepareRunTask);
            task.setWorkingDir(runConfiguration.getWorkingDirectory());
            task.getMainClass().set(runConfiguration.getMainClass());

            JavaToolchainService service = project.getExtensions().getByType(JavaToolchainService.class);
            task.getJavaLauncher().set(service.launcherFor(project.getExtensions().getByType(JavaPluginExtension.class).getToolchain()));
        })));

    }

    // Workaround for the issue where file paths with spaces are improperly split into multiple args.
    protected static String fixupArg(String replace) {
        if (replace.startsWith("\""))
            return replace;

        if (!replace.contains(" "))
            return replace;

        return '"' + replace + '"';
    }

    protected static Provider<String> getProgramArgumentsAsStatement(RunConfiguration runConfiguration) {
        return getProgramArguments(runConfiguration).map(args -> String.join(" ", args));
    }

    protected static Provider<List<String>> getProgramArguments(RunConfiguration runConfiguration) {
        return getProgramArguments(runConfiguration, true);
    }
    
    protected static Provider<List<String>> getProgramArguments(RunConfiguration runConfiguration, boolean wrapSpaces) {
        if (!wrapSpaces)
            return runConfiguration.getProgramArguments();
        
        return runConfiguration.getProgramArguments().map(args -> args.stream().map(RunConfigGenerator::fixupArg).collect(Collectors.toList()));
    }

    protected static Provider<String> getJvmArgumentsAsStatement(RunConfiguration runConfiguration) {
        return getJvmArguments(runConfiguration).map(args -> String.join(" ", args));
    }

    protected static Provider<List<String>> getJvmArguments(RunConfiguration runConfiguration) {
        return getJvmArguments(runConfiguration, true);
    }

    protected static Provider<List<String>> getJvmArguments(RunConfiguration runConfiguration, boolean wrapSpaces) {
        if (!wrapSpaces)
            return runConfiguration.getJvmArguments();

        return runConfiguration.getJvmArguments().map(args -> args.stream().map(RunConfigGenerator::fixupArg).collect(Collectors.toList()));
    }

    protected static Provider<String> getSystemPropertiesAsStatement(RunConfiguration runConfiguration) {
        return getSystemProperties(runConfiguration).map(args -> args.entrySet().stream().map(entry -> "-D" + entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(" ")));
    }

    protected static Provider<Map<String, String>> getSystemProperties(RunConfiguration runConfiguration) {
        return runConfiguration.getSystemProperties();
    }

    protected static Provider<String> getJvmArgumentsAndSystemPropertiesAsStatement(RunConfiguration runConfiguration) {
        return getJvmArgumentsAndSystemProperties(runConfiguration).map(args -> String.join(" ", args));
    }

    protected static Provider<List<String>> getJvmArgumentsAndSystemProperties(RunConfiguration runConfiguration) {
        return getJvmArgumentsAndSystemProperties(runConfiguration, true);
    }

    protected static Provider<List<String>> getJvmArgumentsAndSystemProperties(RunConfiguration runConfiguration, boolean wrapSpaces) {
        return getJvmArguments(runConfiguration, wrapSpaces).flatMap(jvmArgs -> getSystemProperties(runConfiguration).map(sysProps -> {
            List<String> args = new ArrayList<>(jvmArgs);
            sysProps.forEach((key, value) -> args.add("-D" + key + "=" + value));
            return args;
        })).orElse(getSystemProperties(runConfiguration).map(sysProps -> {
            List<String> args = new ArrayList<>();
            sysProps.forEach((key, value) -> args.add("-D" + key + "=" + value));
            return args;
        }));
    }

    public abstract void createRunConfiguration(@Nonnull final McpMinecraftExtension minecraft, @Nonnull final File runConfigurationsDir, @Nonnull final Project project, List<String> additionalClientArgs);

    static abstract class XMLConfigurationBuilder extends RunConfigGenerator {

        @Nonnull
        protected abstract Map<String, Document> createRunConfiguration(@Nonnull final Project project, @Nonnull final RunConfiguration runConfiguration, @Nonnull final DocumentBuilder documentBuilder, List<String> additionalClientArgs);

        @Override
        public final void createRunConfiguration(@Nonnull final McpMinecraftExtension minecraft, @Nonnull final File runConfigurationsDir, @Nonnull final Project project, List<String> additionalClientArgs) {
            try {
                final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                final Transformer transformer = transformerFactory.newTransformer();

                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

                minecraft.getRunConfigurations().forEach(runConfig -> {
                    final Map<String, Document> documents = createRunConfiguration(project, runConfig, docBuilder, additionalClientArgs);

                    documents.forEach((fileName, document) -> {
                        final DOMSource source = new DOMSource(document);
                        final StreamResult result = new StreamResult(new File(runConfigurationsDir, fileName));

                        try {
                            transformer.transform(source, result);
                        } catch (TransformerException e) {
                            e.printStackTrace();
                        }
                    });
                });
            } catch (ParserConfigurationException | TransformerConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    static abstract class JsonConfigurationBuilder extends RunConfigGenerator {

        @Nonnull
        protected abstract JsonObject createRunConfiguration(@Nonnull final Project project, @Nonnull final RunConfiguration runConfiguration, List<String> additionalClientArgs);

        @Override
        public final void createRunConfiguration(@Nonnull final McpMinecraftExtension minecraft, @Nonnull final File runConfigurationsDir, @Nonnull final Project project, List<String> additionalClientArgs) {
            final JsonObject rootObject = new JsonObject();
            rootObject.addProperty("version", "0.2.0");
            JsonArray runConfigs = new JsonArray();
            minecraft.getRunConfigurations().forEach(runConfig -> {
                runConfigs.add(createRunConfiguration(project, runConfig, additionalClientArgs));
            });
            rootObject.add("configurations", runConfigs);
            Writer writer;
            try {
                writer = new FileWriter(new File(runConfigurationsDir, "launch.json"));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                writer.write(gson.toJson(rootObject));
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
