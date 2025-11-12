package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class AdvancedFmlTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    @Override
    protected File getTestTempDirectory() {
        return new File("tests/runtime")
    }

    def "a mod with userdev as dependency can run the patch task for that dependency"() {
        given:
        def project = create("running_patch_task_is_possible", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':neoFormRecompile')
            it.stacktrace()
        }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }

    def "a mod with userdev as dependency can run the compile task for that dependency in ci mode"() {
        given:
        def project = create("running_compile_in_ci", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.withEnvironmentVariable("CI", "true")
        })

        when:
        def run = project.run {
            it.tasks(':compileJava')
            it.stacktrace()
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.NO_SOURCE
        run.task(":neoFormDecompile") == null
    }

    def "a mod with userdev as dependency can run the compile task for that dependency with the decompiler disabled"() {
        given:
        def project = create("running_compile_in_ci", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def run = project.run {
            it.tasks(':compileJava')
            it.stacktrace()
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.NO_SOURCE
        run.task(":neoFormDecompile") == null
    }

    def "a mod using a disabled decompiler should be able to run the game"() {
        given:
        def project = create("version_libs_runnable", {
            it.file("gradle/libs.versions.toml",
                    """
                    [versions]
                    # Neoforge Settings
                    neoforge = "+"
                    
                    [libraries]
                    neoforge = { group = "net.neoforged", name = "neoforge", version.ref = "neoforge" }
                    """.trim())

            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
                        
            dependencies {
                implementation(libs.neoforge)
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def run = project.run {
            it.tasks(':runClientData')
            //We are expecting this test to fail, since there is a mod without any files included so it is fine.
            it.shouldFail()
            it.stacktrace()
        }

        then:
        true
        run.output.contains("is not a valid mod file")
        run.task(":neoFormDecompile") == null
    }
}
