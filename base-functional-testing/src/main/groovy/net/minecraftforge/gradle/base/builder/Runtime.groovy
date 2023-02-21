package net.minecraftforge.gradle.base.builder

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner

import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.function.Consumer
import java.util.stream.Collectors

class Runtime {

    private final String projectName
    private final Map<String, String> properties = Maps.newHashMap();
    private final boolean usesLocalBuildCache;
    private final Map<String, String> files;

    private File projectDir
    private Runtime rootProject

    Runtime(String projectName, Map<String, String> properties, boolean usesLocalBuildCache, Map<String, String> files) {
        this.projectName = projectName
        this.usesLocalBuildCache = usesLocalBuildCache
        this.files = files

        this.properties.put('org.gradle.console', 'rich')
        this.properties.putAll(properties)
    }

    private GradleRunner gradleRunner() {
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir)
                .forwardOutput()

        return runner
    }

    void setup(final Runtime rootProject, final File workspaceDirectory) {
        if (rootProject == this) {
            setupThisAsRoot(workspaceDirectory)
        } else {
            setupThisAsChild(rootProject, workspaceDirectory)
        }
    }

    private void setupThisAsChild(final Runtime rootProject, final File workspaceDirectory) {
        this.projectDir = new File(workspaceDirectory, this.projectName.replace(":", "/"))
        this.projectDir.mkdirs();
        this.rootProject = rootProject;

        setupThis();
    }

    private void setupThisAsRoot(final File workspaceDirectory) {
        this.projectDir = workspaceDirectory;
        this.rootProject = this;

        final File settingsFile = new File(this.projectDir, "settings.gradle");
        settingsFile.getParentFile().mkdirs();
        if (this.usesLocalBuildCache) {
            final File localBuildCacheDirectory = new File(this.projectDir, "cache/build");
            settingsFile << """
                buildCache {
                    local {
                        directory '${localBuildCacheDirectory.toURI()}'
                    }
                }
            """
        }

        setupThis()
    }

    private void setupThis() {
        final File propertiesFile = new File(this.projectDir, 'gradle.properties')
        propertiesFile.getParentFile().mkdirs();
        Files.write(propertiesFile.toPath(), this.properties.entrySet().stream().map {e -> "${e.getKey()}=$e.value".toString() }.collect(Collectors.toList()), StandardOpenOption.CREATE_NEW)

        this.files.forEach {(String file, String content) -> {
            final File target = new File(this.projectDir, file);
            target.getParentFile().mkdirs();
            target << content;
        }}
    }

    BuildResult run(final Consumer<RunBuilder> runBuilderConsumer) {
        if (this.rootProject != this)
            throw new IllegalStateException("Tried to run none root build!");

        final RunBuilder runBuilder = new RunBuilder()
        runBuilderConsumer.accept(runBuilder)

        final GradleRunner runner = gradleRunner()

        final List<String> arguments = Lists.newArrayList(runBuilder.tasks)
        arguments.addAll(runBuilder.arguments)
        arguments.addAll(runBuilder.logLevel.getArgument())

        if (runBuilder.shouldFail) {
            return runner.withArguments(arguments).buildAndFail()
        } else {
            return runner.withArguments(arguments).build()
        }
    }

    String getProjectName() {
        return projectName
    }

    static class Builder {
        private final String projectName

        private final Map<String, String> properties = Maps.newHashMap();
        private final Map<String, String> files = Maps.newHashMap();

        private boolean usesLocalBuildCache = true;

        Builder(String projectName) {
            this.projectName = projectName
        }

        Builder disableLocalBuildCache() {
            this.usesLocalBuildCache = false
            return this
        }

        Builder property(final String key, final String value) {
            this.properties.put(key, value)
            return this
        }

        Builder file(final String path, final String content) {
            this.files.put(path, content)
            return this
        }

        Builder build(final String content) {
            return this.file("build.gradle", content)
        }

        Runtime create() {
            return new Runtime(this.projectName, this.properties, this.usesLocalBuildCache, this.files);
        }
    }

    static class RunBuilder {
        private LogLevel logLevel = LogLevel.NONE
        private final List<String> arguments = new ArrayList<>()
        private final List<String> tasks = new ArrayList<>()
        private boolean shouldFail = false

        private RunBuilder() {
        }

        RunBuilder log(final LogLevel level) {
            this.logLevel = level
            return this
        }

        RunBuilder arguments(final String... args) {
            this.arguments.addAll(args)
            return this
        }

        RunBuilder tasks(final String... tsk) {
            this.tasks.addAll(tsk)
            return this
        }

        RunBuilder shouldFail() {
            this.shouldFail = true
            return this
        }
    }

    static enum LogLevel {
        NONE(""),
        INFO("info"),
        DEBUG("debug");

        private final String argument

        LogLevel(String argument) {
            this.argument = argument
        }

        String getArgument() {
            if (argument.isEmpty())
                return ''

            return "--$argument"
        }
    }
}
