package net.neoforged.gradle.vanilla

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class VersionRangeTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.vanilla";
        injectIntoAllProject = true;
    }

    def "the vanilla runtime supports finding an mc version from a range"() {
        given:
        def project = create("vanilla_loads_from_a_range", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            dependencies {
                implementation ('net.minecraft:client') {
                    version {
                        strictly '[1.21.6,1.21.7)'
                    }
                } 
            }
            """)

            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('assemble')
            it.stacktrace()
        }

        then:
        initialRun.task(":assemble").outcome == TaskOutcome.SUCCESS
    }

    def "the_vanilla_runtime_supports_loading_a_range_with_a_preferred_option_chosen"() {
        given:
        def project = create("vanilla_supports_ats_from_file", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)

            dependencies {
                implementation ('net.minecraft:client') {
                    version {
                        strictly '[1.21.6,1.21.7)'
                        prefer '1.21.6'
                    }
                } 
            }
            """)

            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def initialRun = project.run {
            it.tasks('assemble')
            it.stacktrace()
        }

        then:
        initialRun.task(":assemble").outcome == TaskOutcome.SUCCESS
        initialRun.task(":cacheVersionManifest1.21.6").outcome == TaskOutcome.SUCCESS
    }
}
