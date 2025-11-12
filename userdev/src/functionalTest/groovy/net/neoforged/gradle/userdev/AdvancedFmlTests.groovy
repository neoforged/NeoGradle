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
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
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
                implementation 'net.neoforged:neoforge:+'
            }
            """)

            it.file("src/main/resources/META-INF/neoforge.mods.toml", """
            license="MIT"
            [[mods]]
            modId="neogradle_test"
            version="1.0"
            displayName="NeoGradle Test"
            description='''Test Mod for NeoGradle'''
            """.stripMargin())

            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.fml.common.Mod;
                
                @Mod("neogradle_test")
                public class FunctionalTests {
                    public FunctionalTests() {
                        System.out.println(Minecraft.class.toString());
                    }
                }
            """.stripMargin())
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property("neogradle.subsystems.decompiler.enabled", "false")
        })

        when:
        def run = project.run {
            it.tasks(':runClientData')
            it.stacktrace()
        }

        then:
        true
        run.output.contains("class net.minecraft.client.Minecraft")
        run.task(":neoFormDecompile") == null
    }
}
