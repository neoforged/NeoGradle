package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationCacheTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }


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
        })

        when:
        def run = project.run {
            it.tasks('build')
            it.arguments('--configuration-cache', '--build-cache')
        }

        then:
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }

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
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
            it.arguments('--configuration-cache')
        }

        and:
        def secondaryRun = project.run {
            it.tasks('compileJava')
            it.arguments('--configuration-cache')
        }

        then:
        secondaryRun.output.contains('Reusing configuration cache.')
        run.task(':neoFormDecompile').outcome == TaskOutcome.SUCCESS
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        secondaryRun.task(':neoFormDecompile').outcome == TaskOutcome.FROM_CACHE
        secondaryRun.task(':compileJava').outcome == TaskOutcome.FROM_CACHE
    }
}
