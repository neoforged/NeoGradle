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
            it.stacktrace()
        }

        then:
        run.task(':clean').outcome == TaskOutcome.SUCCESS
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "a mod with userdev as dependency and official mappings has the client-extra jar as a dependency"() {
        given:
        def project = create("gradle_userdev_references_client", {
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
            it.tasks('dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("net.minecraft:client")
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
            it.stacktrace()
        }

        then:
        secondRun.task(":neoFormRecompile").outcome == TaskOutcome.FROM_CACHE
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

    def "the userdev runtime supports restricted repositories"() {
        given:
        def project = create("userdev_supports_loading_from_buildcache", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            //Note: This cannot be static so that fg.repository can be properly accessed
            void exclusiveRepo(RepositoryHandler handler, String url, String... groups) {
                handler.exclusiveContent {
                    it.forRepositories(handler.maven {
                        setUrl(url)
                    })
                    it.filter { f ->
                        for (def group : groups) {
                            f.includeGroup(group)
                        } 
                    }
                }
            }
            
            repositories {
                exclusiveRepo(it, 'https://maven.tterrag.com/', 'team.chisel.ctm')
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
            it.stacktrace()
        }

        then:
        secondRun.task(":neoFormRecompile").outcome == TaskOutcome.FROM_CACHE
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS
    }

}
