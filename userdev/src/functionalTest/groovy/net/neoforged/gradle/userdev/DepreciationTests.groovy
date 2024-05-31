package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class DepreciationTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }


    def "apply_does_not_use_depreciated_resources"() {
        given:
        def project = create("apply_supports_configuration_cache_build", {
            it.build("""
            plugins {
                id 'net.neoforged.gradle.userdev'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('build')
            it.arguments('--warning-mode', 'fail', '--stacktrace')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "compile_does_not_use_depreciated_apis"() {
        given:
        def project = create("compile_supports_configuration_cache_build", {
            it.build("""
            plugins {
                id 'net.neoforged.gradle.userdev'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/userdev/ConfigurationCacheTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class ConfigurationCacheTests {
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
            it.arguments('--warning-mode', 'fail', '--stacktrace')
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
    }
}
