package net.neoforged.gradle.userdev


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationCacheTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "assemble_supports_configuration_cache_build"() {
        given:
        def project = create("assemble_supports_configuration_cache_build", {
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
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
            it.enableBuildScan()
        })

        when:
        def run = project.run {
            it.tasks('build') 
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
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
        })

        when:
        def run = project.run {
            it.tasks('build')
        }

        and:
        def secondaryRun = project.run {
            it.tasks('build')
        }

        and:
        def thirdRun = project.run {
            it.tasks('build')
        }

        then:
        thirdRun.output.contains('Reusing configuration cache.')
        run.task(':neoFormDecompile').outcome == TaskOutcome.SUCCESS
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        thirdRun.task(':neoFormDecompile').outcome == TaskOutcome.FROM_CACHE
        thirdRun.task(':compileJava').outcome == TaskOutcome.FROM_CACHE
    }

    @Override
    protected File getTestTempDirectory() {
        return new File("build/test-temp")
    }

    def "run_tasks_supports_configuration_cache_build"() {
        given:
        def project = create("compile_supports_configuration_cache_build", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                data { }
            }
            
            afterEvaluate {
                //We don't care for the error here, we just want to run the task so that the config cache is created
                tasks.withType(JavaExec).named('runData') {
                    ignoreExitValue = true
                    group = 'run'
                }
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
            it.enableLocalBuildCache()
        })

        when:
        def run = project.run {
            it.tasks('runData')
            
        }

        then:
        run.task(':runData').outcome == TaskOutcome.SUCCESS
    }
}
