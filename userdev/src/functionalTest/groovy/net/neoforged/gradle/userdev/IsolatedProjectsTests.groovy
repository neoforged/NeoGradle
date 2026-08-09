package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

/**
 * Tests for Gradle Isolated Projects support.
 *
 * CURRENT STATUS: BASIC SUPPORT (single-project configuration phase works, runServer executes)
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
 *
 * Fixes applied (2026-08-09):
 * 4. CommonProjectPlugin: Removed redundant cross-project pluginManager access for IdeaExtPlugin.
 *    The plugin was being applied on root project from child projects, violating isolated projects rules.
 *
 * 5. IdeManagementExtension: Made rootProject reference lazy (via getter instead of field) and restricted
 *    IdeaExtPlugin application to root project only. The JetBrains IdeaExtPlugin internally accesses
 *    Project.file() on other projects during apply(), which is not allowed for subprojects under isolated
 *    projects mode.
 *
 * 6. IdeRunIntegrationManager.setup(): Added root-project guard so that IDEA model configuration (which
 *    accesses root project extensions) only runs on the root project, avoiding cross-project access from
 *    child projects during plugin apply time.
 *
 * KNOWN ISSUES:
 * - Multi-project builds with modSource referencing another project's sourceSets fail with error:
 *   "Project ':mod' cannot access 'sourceSets' extension on another project ':api'"
 *   This is a Gradle isolated projects constraint — build scripts cannot directly access extensions
 *   (like sourceSets) on other projects. The test "multiple projects with neoforge dependencies should be
 *   able to run the game" documents this failure case. To support multi-project runs under isolated
 *   projects mode, NeoGradle would need an alternative mechanism for specifying modSource that doesn't
 *   require direct cross-project sourceSets access (e.g., via a dedicated extension or convention).
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
        run.checkModLoading()
    }

    def "multiple projects with neoforge dependencies should be able to run the game"() {
        given:
        def rootProject = create("isolated_multi_neoforge_root", {
            it.withRoot()
            // Enable isolated projects via gradle.properties
            it.property('org.gradle.isolated-projects', 'true')
        })

        def apiProject = create(rootProject, "api", {
            it.withSimpleLibrary()
            it.plugin(this.pluginUnderTest)
        })

        def modProject = create(rootProject, "mod", {
            // Use variant-based mod source dependencies for isolated projects compatibility.
            // This avoids direct cross-project extension access during configuration time by using
            // Gradle's standard dependency notation with consumable configurations.
            it.withRun("""
            runs {
                server {
                    dependencies {
                        // Declare :api's main source set as a mod source.
                        // The 'modSource' prefix is applied automatically — no need to specify 'modSourceMain'.
                        // This is validated immediately by Gradle — fails early if project or config doesn't exist.
                        modSource project(path: ':api', configuration: 'main')
                    }
                }
            }
            """)
            it.plugin(this.pluginUnderTest)
        })

        when:
        def run = rootProject.run {
            it.run(":mod")
            it.gradleVersion("9.7.0")  // Use Gradle 9.7.0 for isolated projects support
            it.stacktrace()
        }

        then:
        run.checkModLoading(":mod")
    }

    def "multiple projects with neoforge dependencies should be able to run the game using standard java plugin configurations"() {
        given:
        def rootProject = create("isolated_multi_neoforge_root_standard_config", {
            it.withRoot()
            // Enable isolated projects via gradle.properties
            it.property('org.gradle.isolated-projects', 'true')
        })

        def apiProject = create(rootProject, "api", {
            it.withSimpleLibrary()
            it.plugin(this.pluginUnderTest)
        })

        def modProject = create(rootProject, "mod", {
            // Use standard Java plugin configurations instead of custom modSource* configs.
            // The Java plugin automatically creates consumable configurations named after each source set
            // (e.g., "main", "test") that expose the source set's output. This provides a better developer
            // experience as users don't need to know about NeoGradle-specific configuration names.
            it.withRun("""
            runs {
                server {
                    dependencies {
                        // Use standard Java plugin config 'main' instead of 'modSourceMain'.
                        // Gradle validates this immediately — fails early if project or config doesn't exist.
                        modSource project(path: ':api', configuration: 'main')
                    }
                }
            }
            """)
            it.plugin(this.pluginUnderTest)
        })

        when:
        def run = rootProject.run {
            it.run(":mod")
            it.gradleVersion("9.7.0")  // Use Gradle 9.7.0 for isolated projects support
            it.stacktrace()
        }

        then:
        run.checkModLoading(":mod")
    }
}
