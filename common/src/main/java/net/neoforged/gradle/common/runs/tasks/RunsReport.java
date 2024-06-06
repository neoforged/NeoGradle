package net.neoforged.gradle.common.runs.tasks;

import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.diagnostics.AbstractProjectBasedReportTask;
import org.gradle.api.tasks.diagnostics.internal.ProjectDetails;
import org.gradle.api.tasks.diagnostics.internal.ReportRenderer;
import org.gradle.api.tasks.diagnostics.internal.TextReportRenderer;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.work.DisableCachingByDefault;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@DisableCachingByDefault(
        because = "Not worth caching"
)
public abstract class RunsReport extends AbstractProjectBasedReportTask<RunsReport.RunsProjectReport> {

    private final Renderer renderer = new Renderer();

    @Override
    protected ReportRenderer getRenderer() {
        return renderer;
    }

    @Override
    protected void generateReportFor(@NotNull ProjectDetails project, @NotNull RunsReport.RunsProjectReport model) {
        model.getRuns().forEach(renderer::renderRun);
        renderer.completeProject(project);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected @NotNull RunsReport.RunsProjectReport calculateReportModelFor(@NotNull Project project) {
        final NamedDomainObjectContainer<Run> runs = (NamedDomainObjectContainer<Run>) project.getExtensions().getByName(RunsConstants.Extensions.RUNS);
        final RunsProjectReport report = new RunsProjectReport();
        runs.forEach(run -> {
            report.addRun(new RenderableRun(run));
        });
        return report;
    }

    public static class RunsProjectReport {

        private final List<RenderableRun> runs;

        public RunsProjectReport() {
            runs = new ArrayList<>();
        }

        public void addRun(RenderableRun run) {
            runs.add(run);
        }

        public List<RenderableRun> getRuns() {
            return runs;
        }
    }

    public static class RenderableRun {
        private final String name;
        private final Map<String, String> environment;
        private final String mainClass;
        private final boolean shouldBuildAllProjects;
        private final Map<String, String> properties;
        private final List<String> programArguments;
        private final List<String> jvmArguments;
        private final boolean isSingleInstance;
        private final String workingDirectory;
        private final boolean isClient;
        private final boolean isServer;
        private final boolean isData;
        private final boolean isJUnit;
        private final boolean isGameTest;
        private final Multimap<String, SourceSet> modSources;
        private final Multimap<String, SourceSet> unitTestSources;
        private final Set<String> classpath;
        private final Set<String> dependencies;

        public RenderableRun(Run run) {
            this.name = run.getName();
            this.environment = run.getEnvironmentVariables().get();
            this.mainClass = run.getMainClass().get();
            this.shouldBuildAllProjects = run.getShouldBuildAllProjects().get();
            this.properties = run.getSystemProperties().get();
            this.programArguments = run.getProgramArguments().get();
            this.jvmArguments = run.getJvmArguments().get();
            this.isSingleInstance = run.getIsSingleInstance().get();
            this.workingDirectory = run.getWorkingDirectory().get().getAsFile().getAbsolutePath();
            this.isClient = run.getIsClient().get();
            this.isServer = run.getIsServer().get();
            this.isData = run.getIsDataGenerator().get();
            this.isJUnit = run.getIsJUnit().get();
            this.isGameTest = run.getIsGameTest().get();
            this.modSources = run.getModSources().all().get();
            this.unitTestSources = run.getUnitTestSources().all().get();
            this.classpath = run.getClasspath().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet());
            this.dependencies = run.getDependencies().get().getRuntimeConfiguration().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet());
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getEnvironment() {
            return environment;
        }

        public String getMainClass() {
            return mainClass;
        }

        public boolean isShouldBuildAllProjects() {
            return shouldBuildAllProjects;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public List<String> getProgramArguments() {
            return programArguments;
        }

        public List<String> getJvmArguments() {
            return jvmArguments;
        }

        public boolean isSingleInstance() {
            return isSingleInstance;
        }

        public String getWorkingDirectory() {
            return workingDirectory;
        }

        public boolean isClient() {
            return isClient;
        }

        public boolean isServer() {
            return isServer;
        }

        public boolean isData() {
            return isData;
        }

        public boolean isJUnit() {
            return isJUnit;
        }

        public boolean isGameTest() {
            return isGameTest;
        }

        public Multimap<String, SourceSet> getModSources() {
            return modSources;
        }

        public Multimap<String, SourceSet> getUnitTestSources() {
            return unitTestSources;
        }

        public Set<String> getClasspath() {
            return classpath;
        }

        public Set<String> getDependencies() {
            return dependencies;
        }
    }

    public static class Renderer extends TextReportRenderer {

        private boolean hasRuns = false;

        @Override
        public void completeProject(ProjectDetails project) {
            super.completeProject(project);
            if (!hasRuns) {
                getTextOutput().text("  - No Runs");
            }
        }

        private void outputHeader(String header) {
            getTextOutput().withStyle(StyledTextOutput.Style.Header).text(header);
            outputNewLine();
            outputNormal("----------------------------------------------------------");
        }

        private void outputIdentifier(String identifier) {
            getTextOutput().withStyle(StyledTextOutput.Style.Identifier).text(identifier);
        }

        private void outputNormal(String normal) {
            getTextOutput().withStyle(StyledTextOutput.Style.Normal).text(normal);
        }

        private void outputError(String error) {
            getTextOutput().withStyle(StyledTextOutput.Style.Error).text(error);
        }

        private void outputNewLine() {
            getTextOutput().println();
        }

        private void renderRun(RenderableRun run) {
            if (hasRuns) {
                getTextOutput().println();
            }

            outputHeader("Run: " + run.getName());
            outputNewLine();
            outputIdentifier("Main Class:");
            outputNormal(run.getMainClass());
            outputNewLine();
            outputIdentifier("Should Build All Projects:");
            outputNormal(String.valueOf(run.isShouldBuildAllProjects()));
            outputNewLine();
            outputIdentifier("Working Directory:");
            outputNormal(run.getWorkingDirectory());
            outputNewLine();
            outputIdentifier("Is Single Instance:");
            outputNormal(String.valueOf(run.isSingleInstance()));
            outputNewLine();
            outputIdentifier("Is Client:");
            outputNormal(String.valueOf(run.isClient()));
            outputNewLine();
            outputIdentifier("Is Server:");
            outputNormal(String.valueOf(run.isServer()));
            outputNewLine();
            outputIdentifier("Is Data Generator:");
            outputNormal(String.valueOf(run.isData()));
            outputNewLine();
            outputIdentifier("Is JUnit:");
            outputNormal(String.valueOf(run.isJUnit()));
            outputNewLine();
            outputIdentifier("Is Game Test:");
            outputNormal(String.valueOf(run.isGameTest()));
            outputNewLine();

            renderEnvironment(run.getEnvironment());
            renderProperties(run.getProperties());
            renderProgramArguments(run.getProgramArguments());
            renderJvmArguments(run.getJvmArguments());
            renderModSources(run.getModSources());
            renderUnitTestSources(run.getUnitTestSources());
            renderClasspath(run.getClasspath());
            renderDependencies(run.getDependencies());

            outputNewLine();
            outputNewLine();
        }

        private void renderEnvironment(Map<String, String> environment) {
            outputIdentifier("Environment Variables:");
            outputNewLine();

            if (environment.isEmpty()) {
                outputNormal("  - No Environment Variables");
                outputNewLine();
            } else {
                environment.forEach((key, value) -> {
                    outputIdentifier("  - " + key + ":");
                    outputNormal(value);
                    outputNewLine();
                });
            }
        }

        private void renderProperties(Map<String, String> properties) {
            outputIdentifier("System Properties:");
            outputNewLine();

            if (properties.isEmpty()) {
                outputNormal("  - No System Properties");
                outputNewLine();
            } else {
                properties.forEach((key, value) -> {
                    outputIdentifier("  - " + key + ":");
                    outputNormal(value);
                    outputNewLine();
                });
            }
        }

        private void renderProgramArguments(List<String> programArguments) {
            outputIdentifier("Program Arguments:");
            outputNewLine();

            if (programArguments.isEmpty()) {
                outputNormal("  - No Program Arguments");
                outputNewLine();
            } else {
                programArguments.forEach(arg -> {
                    outputNormal("  - " + arg);
                    outputNewLine();
                });
            }
        }

        private void renderJvmArguments(List<String> jvmArguments) {
            outputIdentifier("JVM Arguments:");
            outputNewLine();

            if (jvmArguments.isEmpty()) {
                outputNormal("  - No JVM Arguments");
                outputNewLine();
            } else {
                jvmArguments.forEach(arg -> {
                    outputNormal("  - " + arg);
                    outputNewLine();
                });
            }
        }

        private void renderModSources(Multimap<String, SourceSet> modSources) {
            outputIdentifier("Mod Sources:");
            outputNewLine();

            if (modSources.isEmpty()) {
                outputError("  - No Mod Sources");
                outputNewLine();
            } else {
                modSources.keySet().forEach(projectId -> {
                    outputIdentifier("  - " + projectId + ":");
                    outputNewLine();
                    final Collection<SourceSet> sourceSets = modSources.get(projectId);
                    if (sourceSets.isEmpty()) {
                        outputNormal("    - No Source Sets");
                        outputNewLine();
                    } else {
                        sourceSets.forEach(sourceSet -> {
                            outputNormal("    - " + sourceSet.getName());
                            outputNewLine();
                        });
                    }
                });
            }
        }

        private void renderUnitTestSources(Multimap<String, SourceSet> unitTestSources) {
            outputIdentifier("Unit Test Sources:");
            outputNewLine();

            if (unitTestSources.isEmpty()) {
                outputError("  - No Unit Test Sources");
                outputNewLine();
            } else {
                unitTestSources.keySet().forEach(projectId -> {
                    outputIdentifier("  - " + projectId + ":");
                    outputNewLine();
                    final Collection<SourceSet> sourceSets = unitTestSources.get(projectId);
                    if (sourceSets.isEmpty()) {
                        outputNormal("    - No Source Sets");
                        outputNewLine();
                    } else {
                        sourceSets.forEach(sourceSet -> {
                            outputNormal("    - " + sourceSet.getName());
                            outputNewLine();
                        });
                    }
                });
            }
        }

        private void renderClasspath(Set<String> classpath) {
            outputIdentifier("Classpath:");
            outputNewLine();

            if (classpath.isEmpty()) {
                outputError("  - No Classpath entries");
                outputNewLine();
            } else {
                classpath.forEach(path -> {
                    outputNormal("  - " + path);
                    outputNewLine();
                });
            }
        }

        private void renderDependencies(Set<String> dependencies) {
            outputIdentifier("Dependencies:");
            outputNewLine();

            if (dependencies.isEmpty()) {
                outputError("  - No Dependencies");
                outputNewLine();
            } else {
                dependencies.forEach(dependency -> {
                    outputNormal("  - " + dependency);
                    outputNewLine();
                });
            }
        }
    }
}
