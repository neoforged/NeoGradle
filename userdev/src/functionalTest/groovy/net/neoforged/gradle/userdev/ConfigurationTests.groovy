package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "a mod with a default run and without run dependencies does not have them in the classpath"() {
        given:
        def project = create("no_run_dep_results_in_no_entry", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation 'org.jgrapht:jgrapht-core:+'
            }
            
            runs {
                client {
                    modSources.add(project.getSourceSets().main)
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
        })

        when:
        def run = project.run {
            it.tasks('writeMinecraftClasspathClient')
        }

        then:


        def neoFormDirectory = run.file('.gradle/configuration/neoForm')
        def versionedNeoFormDirectory = neoFormDirectory.listFiles()[0];
        def stepsDirectory = new File(versionedNeoFormDirectory, 'steps')
        def targetDirectory = new File(stepsDirectory, 'writeMinecraftClasspathClient')
        def classpathFile = new File(targetDirectory, 'classpath.txt')

        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS
        classpathFile.exists()
        !classpathFile.text.contains('jgrapht')
    }

    def "a mod with a default run and with run dependencies does have them in the classpath"() {
        given:
        def project = create("run_dep_results_in_entry", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                implementation 'org.jgrapht:jgrapht-core:+'
            }
            
            runs {
                client {
                    modSources.add(project.getSourceSets().main)
                    
                    dependencies {
                        runtime 'org.jgrapht:jgrapht-core:+'
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
        })

        when:
        def run = project.run {
            it.tasks('writeMinecraftClasspathClient')
        }

        then:


        def neoFormDirectory = run.file('.gradle/configuration/neoForm')
        def versionedNeoFormDirectory = neoFormDirectory.listFiles()[0];
        def stepsDirectory = new File(versionedNeoFormDirectory, 'steps')
        def targetDirectory = new File(stepsDirectory, 'writeMinecraftClasspathClient')
        def classpathFile = new File(targetDirectory, 'classpath.txt')

        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS
        classpathFile.exists()
        classpathFile.text.contains('jgrapht')
    }

    def "a mod with userdev in the implementation configuration does not leak it"() {
        given:
        def project = create("userdev_in_implementation_does_not_leak", {
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
                client {
                    modSources.add(project.getSourceSets().main)
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
        })

        when:
        def run = project.run {
            it.tasks('dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains(
'''
implementation - Implementation dependencies for the 'main' feature. (n)
No dependencies
'''
        )
    }
}
