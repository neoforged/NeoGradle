package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.caching.CentralCacheService
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
            
            compileDependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':neoFormRecompile')
        }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }

    def "userdev supports version range resolution"() {
        given:
        def project = create("userdev_supports_version_ranges", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            compileDependencies {
                implementation 'net.neoforged:neoforge:[20,)'
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
        })

        when:
        def run = project.run {
            it.tasks('compileDependencies', "--configuration", "compileClasspath")
        }

        then:
        run.task(':compileDependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("+--- net.neoforged.fancymodloader:loader:")
    }

    def "userdev supports complex version resolution"() {
        given:
        def project = create("userdev_supports_complex_versions", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            compileDependencies {
                implementation ('net.neoforged:neoforge') {
                    version {
                        strictly '[20.4.167, 20.5)'
                        require '20.4.188'
                    }
                }
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
        })

        when:
        def run = project.run {
            it.tasks('compileDependencies', "--configuration", "compileClasspath")
        }

        then:
        run.task(':compileDependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("+--- net.neoforged.fancymodloader:loader:")
    }

    def "a mod with userdev as dependency has a mixin-extra dependency on the compile classpath"() {
        given:
        def project = create("userdev_adds_mixin_extra_on_compile_classpath", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            compileDependencies {
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
        })

        when:
        def run = project.run {
            it.tasks('compileDependencies', "--configuration", "compileClasspath")
        }

        then:
        run.task(':compileDependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("+--- io.github.llamalad7:mixinextras-neoforge")
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
            
            compileDependencies {
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
            
            compileDependencies {
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

    def "the userdev runtime by default supports the build cache"() {
        given:
        def project = create("userdev_supports_loading_from_buildcache", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            compileDependencies {
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
            it.withGlobalCacheDirectory(tempDir)
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
            
            repositories {
                exclusiveContent {
                    forRepository {
                        maven { url 'https://maven.tterrag.com/' }
                    }
                    filter { includeGroup('team.chisel.ctm') }
                }
            }
            
            compileDependencies {
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
            it.withGlobalCacheDirectory(tempDir)
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

    def "a mod with userdev can have multiple sourcesets with neoforge"() {
        given:
        def project = create("gradle_multi_sourceset", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            sourceSets {
                content {
                    java {
                        srcDir 'src/content/java'
                    }
                }
            }
            
            compileDependencies {
                implementation 'net.neoforged:neoforge:+'
                contentImplementation 'net.neoforged:neoforge:+'
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
            it.file("src/content/java/net/neoforged/gradle/userdev/ContentTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class ContentTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
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
}
