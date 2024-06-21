package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
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
            
            compileDependencies {
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
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('compileDependencies')
        }

        then:
        run.task(':compileDependencies').outcome == TaskOutcome.SUCCESS
        def implementationStartLine = run.output.split(System.lineSeparator()).toList().indexOf('implementation - Implementation compileDependencies for the \'main\' feature. (n)')
        implementationStartLine != -1
        def nextLine = run.output.split(System.lineSeparator()).toList().get(implementationStartLine + 1)
        nextLine.contains("No compileDependencies")
    }

    def "a mod with userdev in the implementation configuration does not leak it via publishing"() {
        given:
        def project = create("userdev_in_implementation_does_not_leak_via_publishing", {
            it.plugin('maven-publish')
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            compileDependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    modSources.add(project.getSourceSets().main)
                }
            }
            
            publishing {
                publications {
                    maven(MavenPublication) {
                        from components.java
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
            it.tasks('generateMetadataFileForMavenPublication', 'generatePomFileForMavenPublication')
        }

        then:
        run.task(':generateMetadataFileForMavenPublication').outcome == TaskOutcome.SUCCESS
        !run.file('build/publications/maven/module.json').text.contains("compileDependencies")
        !run.file('build/publications/maven/pom-default.xml').text.contains("compileDependencies")
    }
}
