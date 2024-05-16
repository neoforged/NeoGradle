package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
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
        })

        when:
        def run = project.run {
            it.tasks(':neoFormRecompile')
        }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }

    def "a mod with userdev as dependency and official mappings can compile through gradle"() {
        given:
        def project = create("compile_with_gradle_and_official_mappings", {
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
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
    }

    def "a mod with userdev as dependency and official mappings can run build and clean in the same execution"() {
        given:
        def project = create("gradle_userdev_clean_build", {
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
        })

        when:
        def run = project.run {
            it.tasks('clean', 'build')
        }

        then:
        run.task(':clean').outcome == TaskOutcome.SUCCESS
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime by default supports the build cache"() {
        given:
        def project = create("userdev_supports_loading_from_buildcache", {
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
            it.enableLocalBuildCache()
            it.debugBuildCache()
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS

        and:
        def secondRun = project.run {
            it.tasks('build')
        }

        then:
        secondRun.task(":neoFormRecompile").outcome == TaskOutcome.FROM_CACHE
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

}
