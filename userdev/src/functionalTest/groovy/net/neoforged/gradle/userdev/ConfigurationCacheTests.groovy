package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationCacheTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }


    def "apply_supports_configuration_cache_build"() {
        given:
        def project = create("apply_supports_configuration_cache_build", {
            it.build("""
            plugins {
                id 'net.neoforged.gradle.userdev'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
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
            it.tasks('build')
            it.arguments('-Dorg.gradle.configuration-cache=true', '--build-cache')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    def "compile_supports_configuration_cache_build"() {
        given:
        def project = create("compile_supports_configuration_cache_build", {
            it.build("""
            plugins {
                id 'net.neoforged.gradle.userdev'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
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
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
            it.arguments('--configuration-cache', '--build-cache')
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
    }
}
