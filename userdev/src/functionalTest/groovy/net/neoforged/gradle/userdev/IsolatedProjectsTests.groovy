package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

/**
 * Tests for Gradle Isolated Projects support.
 *
 * CURRENT STATUS: BASIC SUPPORT (configuration phase works, runServer executes)
 *
 * Fixes applied (2026-08-08):
 * 1. RunsUtil.addRunSourcesDependenciesToTask(): Removed direct task graph introspection via
 *    getBuildDependencies().getDependencies(null). Now uses lazy task references via project.getTasks().named()
 *    which are allowed under isolated projects constraints.
 *
 * 2. SourceSetUtils.getProject(): Removed fallback to getBuildDependencies().getDependencies(null) when
 *    ProjectHolder extension is missing. NeoGradle always registers ProjectHolder on its source sets,
 *    so the fallback was unnecessary and violated isolated projects rules.
 *
 * 3. DirectoryCache.loadFrom(): Replaced FileUtils.copyDirectory() with NIO-based Files.walkFileTree()
 *    wrapped in a timeout executor. The original implementation could hang indefinitely on macOS with
 *    Gradle test kit temp directories due to native file I/O blocking. Timeout is set to 5 minutes to
 *    accommodate large directory copies (e.g., decompiled Minecraft sources).
 */
class IsolatedProjectsTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "isolated projects support is available for basic runServer"() {
        given:
        def project = create("isolated_projects_test", { builder ->
            // withRun() sets up everything needed for a basic runServer test
            builder.withRun()

            // Enable isolated projects via gradle.properties
            builder.property('org.gradle.isolated-projects', 'true')
        })

        when:
        def run = project.run { rb ->
            // Use TestExtensions.run() helper to target :runServer
            rb.run()
            rb.gradleVersion("9.7.0")  // Use Gradle 9.7.0 for isolated projects support
            rb.stacktrace()
        }

        then:
        // Build should succeed — configuration phase no longer violates isolated projects constraints
        run.task(':runServer').outcome == TaskOutcome.SUCCESS
    }
}
