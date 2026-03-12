package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome
import net.neoforged.gradle.userdev.constants.TestConstants

class AdvancedFmlTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "a mod with userdev as dependency can run the patch task for that dependency"() {
        given:
        def project = create("running_patch_task_is_possible", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            repositories {
                mavenLocal()
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
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            repositories {
                mavenLocal()
            }
                        
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
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
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        run.task(":neoFormDecompile") == null
    }

    def "a mod with userdev as dependency can run the compile task for that dependency with the decompiler disabled"() {
        given:
        def project = create("running_compile_with_compiler_disabled", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            dependencies {
                implementation 'net.neoforged:neoforge:[21.11,21.12)'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
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
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        run.task(":neoFormDecompile") == null
    }

    def "a mod using a disabled decompiler should be able to run the game"() {
        given:
        def project = create("disabled_decompiler_runs_game", {
            it.withRun()
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def run = project.run {
            it.run()
        }

        then:
        run.checkModLoading()
        run.task(":neoFormDecompile") == null
    }

    def "a mod using an enabled decompiler should use the setup to decompile the game"() {
        given:
        def project = create("enabled_decompiler_uses_setup", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            repositories {
                mavenCentral()
            }
                        
            dependencies {
                implementation 'net.neoforged:neoforge:[21.10.60-beta,)'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def run = project.run {
            it.tasks(':neoFormDecompile')
            it.stacktrace()
            it.debug()
        }

        then:
        run.task(":neoFormSetup") != null
    }
}
