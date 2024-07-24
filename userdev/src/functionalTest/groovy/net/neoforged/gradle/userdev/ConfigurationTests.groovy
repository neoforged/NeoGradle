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
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    modSources.add(project.getSourceSets().main)
                }
            }
            
            tasks.named("dependencies", t -> {
                //Force fully check if we have dependencies
                t.doFirst(task -> {
                    def configuration = project.configurations.getByName("implementation")
                    if (configuration.hasDependencies()) {
                        throw new RuntimeException("Still has dependencies")
                    }
                })
            })
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
            it.tasks('dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        def implementationStartLine = run.output.split(System.lineSeparator()).toList().indexOf('implementation - Implementation dependencies for the \'main\' feature. (n)')
        implementationStartLine != -1
        def nextLine = run.output.split(System.lineSeparator()).toList().get(implementationStartLine + 1)
        nextLine.contains("No dependencies")
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
            
            dependencies {
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
        !run.file('build/publications/maven/module.json').text.contains("dependencies")
        !run.file('build/publications/maven/pom-default.xml').text.contains("dependencies")
    }

    def "a mod that lazily adds something to a dependency collector still gets its configuration resolved"() {
        given:
        def project = create("userdev_with_delayed_config_works", {
            it.plugin('maven-publish')
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            public interface ExampleDependencies extends Dependencies {
                DependencyCollector getExample();
            }
            
            public abstract class ExampleExtensions {
                
                @Nested
                public abstract ExampleDependencies getDependencies();
                
                default void dependencies(Action<? super ExampleDependencies> action) {
                    action.execute(getDependencies());
                }
            }
            
            project.getExtensions().create("example", ExampleExtensions)
           
                        
            project.getConfigurations().create("exampleDependencies", conf -> {
                conf.canBeResolved = true
                conf.fromDependencyCollector(project.example.getDependencies().getExample())
            });
           
            
            example.dependencies {
                example("junit:junit:4.12")
            }
            
            tasks.register("validateConfiguration", task -> {
                task.doFirst({
                    project.getConfigurations().getByName("exampleDependencies").getResolvedConfiguration().getFirstLevelModuleDependencies().forEach(dep -> {
                        if (!dep.getModuleGroup().equals("junit")) {
                            throw new RuntimeException("Expected junit dependency, got " + dep.getModuleGroup())
                        }
                    })
                    
                    if (project.getConfigurations().getByName("exampleDependencies").getResolvedConfiguration().getFirstLevelModuleDependencies().size() != 1) {
                        throw new RuntimeException("Expected 1 dependency, got " + project.getConfigurations().getByName("exampleDependencies").getResolvedConfiguration().getFirstLevelModuleDependencies().size())
                    }
                })
            })
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('validateConfiguration')
            it.stacktrace()
        }

        then:
        run.task(':validateConfiguration').outcome == TaskOutcome.SUCCESS
    }
}
