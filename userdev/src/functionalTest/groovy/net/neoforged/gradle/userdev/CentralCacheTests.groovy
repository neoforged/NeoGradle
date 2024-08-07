package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.services.caching.CachedExecutionService
import net.neoforged.gradle.common.services.caching.locking.IOControlledFileBasedLock
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Files

class CentralCacheTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "caching_is_enabled_by_default"() {
        given:
        def project = create("caching_is_enabled_by_default", {
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
            it.property(CachedExecutionService.LOG_CACHE_HITS_PROPERTY, "true")
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('build')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
        run.output.contains("Cache miss for task")
    }

    def "cache_supports_cleanup_and_take_over_of_failed_lock"() {
        given:
        File cacheDir;
        def project = create("cache_supports_cleanup_and_take_over_of_failed_lock", {
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
            cacheDir = it.withGlobalCacheDirectory(tempDir)
            it.property(CachedExecutionService.LOG_CACHE_HITS_PROPERTY, "true")
        })

        if (cacheDir == null) {
            throw new IllegalStateException("Cache directory was not set")
        }

        when:
        project.run {
            it.tasks('build')
            it.stacktrace()
        }

        //Delete all healthy marker files
        Files.walk(cacheDir.toPath())
            .filter { (it.getFileName().toString() == IOControlledFileBasedLock.HEALTHY_FILE_NAME) }
            .forEach { Files.delete(it) }

        def targetRun = project.run {
            it.tasks('build')
        }

        then:
        targetRun.task(':build').outcome == TaskOutcome.SUCCESS
        !targetRun.output.contains("Cache hit for task") //We deleted all healthy markers so we should not have any cache hits.
    }

    def "cache_can_be_disabled"() {
        given:
        def project = create("cache_can_be_disabled", {
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
            it.property(CachedExecutionService.IS_ENABLED_PROPERTY, "false")
            it.property(CachedExecutionService.DEBUG_CACHE_PROPERTY, "true")
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('build')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
        run.output.contains("Caching is disabled, executing all stages.")
    }

    def "updating an AT after cache run should work."() {
        given:
        def project = create("userdev_supports_ats_from_file", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            minecraft.accessTransformers.file rootProject.file('src/main/resources/META-INF/accesstransformer.cfg')
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/resources/META-INF/accesstransformer.cfg", """public-f net.minecraft.client.Minecraft fixerUpper # fixerUpper""")
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().fixerUpper.getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.property(CachedExecutionService.LOG_CACHE_HITS_PROPERTY, "true")
            it.property(CachedExecutionService.DEBUG_CACHE_PROPERTY, "true")
        })

        when:
        def initialRun = project.run {
            it.tasks('build')
        }

        then:
        initialRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        initialRun.task(":build").outcome == TaskOutcome.SUCCESS

        when:
        File atFile = initialRun.file("src/main/resources/META-INF/accesstransformer.cfg")
        atFile.delete()
        atFile << """public-f net.minecraft.client.Minecraft REGIONAL_COMPLIANCIES # REGIONAL_COMPLIANCIES"""

        File codeFile = initialRun.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java")
        codeFile.delete()
        codeFile << """
            package net.neoforged.gradle.userdev;
            
            import net.minecraft.client.Minecraft;
            
            public class FunctionalTests {
                public static void main(String[] args) {
                    System.out.println(Minecraft.getInstance().REGIONAL_COMPLIANCIES.getClass().toString());
                }
            }
        """

        def secondRun = project.run {
            it.tasks('build')
        }

        then:
        secondRun.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS
        secondRun.task(":build").outcome == TaskOutcome.SUCCESS
    }
}
