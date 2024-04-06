package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Disabled

class ConfigurationCacheTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    @Disabled
    def "apply_supports_configuration_cache_build"() {
        given:
        def project = create("apply_supports_configuration_cache_build", {
            it.build("""
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
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
        })

        when:
        def run = project.run {
            it.tasks('build')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

    @Disabled
    def "compile_supports_configuration_cache_build"() {
        given:
        def project = create("compile_supports_configuration_cache_build", {
            it.build("""
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
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
        })

        when:
        def run = project.run {
            it.tasks('build')
        }

        and:
        project.run {
            it.tasks('build')
        }

        and:
        def thirtiaryRun = project.run {
            it.tasks('build')
        }

        then:
        thirtiaryRun.output.contains('Reusing configuration cache.')
        run.task(':neoFormDecompile').outcome == TaskOutcome.SUCCESS
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        thirtiaryRun.task(':neoFormDecompile').outcome == TaskOutcome.FROM_CACHE
        thirtiaryRun.task(':compileJava').outcome == TaskOutcome.FROM_CACHE
    }
}
