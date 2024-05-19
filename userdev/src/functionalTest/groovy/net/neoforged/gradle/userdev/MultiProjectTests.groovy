package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class MultiProjectTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    @Override
    protected File getTestTempDirectory() {
        return new File("build/functionalTest")
    }

    def "multiple projects with neoforge dependencies should be able to run the game"() {
        given:
        def rootProject = create("multi_neoforge_root", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            """)
            it.withToolchains()
        })

        def apiProject = create(rootProject, "api", {
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
            it.file("src/main/java/net/neoforged/gradle/apitest/FunctionalTests.java", """
                package net.neoforged.gradle.apitest;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
        })

        def mainProject = create(rootProject,"main", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation project(':api')
            }
            
            runs {
                client {
                    modSource project(':api').sourceSets.main
                }
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/main/ApiTests.java", """
                package net.neoforged.gradle.main;
                
                import net.minecraft.client.Minecraft;
                import net.neoforged.gradle.apitest.FunctionalTests;
                
                public class ApiTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                        FunctionalTests.main(args);
                    }
                }
            """)
            it.withToolchains()
        })

        when:
        def run = rootProject.run {
            it.tasks(':main:runData')
            //We are expecting this test to fail, since there is a mod without any files included so it is fine.
            it.shouldFail()
        }

        then:
        run.task(':main:writeMinecraftClasspathData').outcome == TaskOutcome.SUCCESS
        run.output.contains("Error during pre-loading phase: ERROR: File null is not a valid mod file") //Validate that we are failing because of the missing mod file, and not something else.
    }
}
