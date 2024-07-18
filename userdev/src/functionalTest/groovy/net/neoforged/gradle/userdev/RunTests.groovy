package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class RunTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "configuring of the configurations after the dependencies block should work"() {
        given:
        def project = create("runs_configuration_after_dependencies", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            sourceSets {
                modRun {
                    java.setSrcDirs(['src/main/mod'])
                    resources.setSrcDirs(['src/main/modResources'])
                }
            }
                        
            dependencies {
                implementation "net.neoforged:neoforge:+"
            }
            
            configurations {
                modRunImplementation.extendsFrom implementation
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':runData')
            //We are expecting this test to fail, since there is a mod without any files included so it is fine.
            it.shouldFail()
        }

        then:
        run.task(':writeMinecraftClasspathData').outcome == TaskOutcome.SUCCESS
        run.output.contains("Error during pre-loading phase: ERROR: File null is not a valid mod file") ||
                run.output.contains("Caused by: java.io.IOException: Invalid paths argument, contained no existing paths")
    }

    def "runs can be declared before the dependencies block"() {
        given:
        def project = create("runs_before_dependencies", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            runs {
                client {
                    modSource project.sourceSets.main
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':tasks')
        }

        then:
        run.task(':tasks').outcome == TaskOutcome.SUCCESS
        run.output.contains('runClient')
    }

    def "when referencing pom on run classpath, it should not list it on the LCP, but it should list its dependencies"() {
        given:
        def project = create("runs_support_poms", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    dependencies {
                        runtime 'org.graalvm.polyglot:python:23.1.2'
                    }
                    
                    modSource project.sourceSets.main
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':writeMinecraftClasspathClient')
        }

        then:
        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS

        def neoformDir = run.file(".gradle/configuration/neoForm")
        def versionedNeoformDir = neoformDir.listFiles()[0]
        def stepsDir = new File(versionedNeoformDir, "steps")
        def stepDir = new File(stepsDir, "writeMinecraftClasspathClient")
        def classpathFile = new File(stepDir, "classpath.txt")

        classpathFile.exists()

        classpathFile.text.contains("org.graalvm.polyglot${File.separator}polyglot")
        !classpathFile.text.contains(".pom")
    }

    def "userdev supports custom run dependencies"() {
        given:
        def project = create("run_with_custom_dependencies", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    dependencies {
                        runtime 'org.jgrapht:jgrapht-core:+'
                    }
                    
                    modSource project.sourceSets.main
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':writeMinecraftClasspathClient')
        }

        then:
        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS

        def neoformDir = run.file(".gradle/configuration/neoForm")
        def versionedNeoformDir = neoformDir.listFiles()[0]
        def stepsDir = new File(versionedNeoformDir, "steps")
        def stepDir = new File(stepsDir, "writeMinecraftClasspathClient")
        def classpathFile = new File(stepDir, "classpath.txt")

        classpathFile.exists()

        classpathFile.text.contains("org.jgrapht${File.separator}jgrapht-core")
    }

    def "userdev supports custom run dependencies from configuration"() {
        given:
        def project = create("run_with_custom_dependencies_from_configuration", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                runRuntime
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
                runRuntime 'org.jgrapht:jgrapht-core:+'
            }
            
            runs {
                client {
                    dependencies {
                        runtime project.configurations.runRuntime
                    }
                    
                    modSource project.sourceSets.main
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':writeMinecraftClasspathClient')
        }

        then:
        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS

        def neoformDir = run.file(".gradle/configuration/neoForm")
        def versionedNeoformDir = neoformDir.listFiles()[0]
        def stepsDir = new File(versionedNeoformDir, "steps")
        def stepDir = new File(stepsDir, "writeMinecraftClasspathClient")
        def classpathFile = new File(stepDir, "classpath.txt")

        classpathFile.exists()

        classpathFile.text.contains("org.jgrapht${File.separator}jgrapht-core")
    }

    def "userdev supports custom run dependencies from catalog"() {
        given:
        def project = create("run_with_custom_dependencies_from_configuration", {
            it.file("gradle/libs.versions.toml",
                    """
                    [libraries]
                    jgrapht = { group = "org.jgrapht", name = "jgrapht-core", version = "+" }
                    """.trim())

            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            configurations {
                runRuntime
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    dependencies {
                        runtime libs.jgrapht
                    }
                    
                    modSource project.sourceSets.main
                }
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':writeMinecraftClasspathClient')
        }

        then:
        run.task(':writeMinecraftClasspathClient').outcome == TaskOutcome.SUCCESS

        def neoformDir = run.file(".gradle/configuration/neoForm")
        def versionedNeoformDir = neoformDir.listFiles()[0]
        def stepsDir = new File(versionedNeoformDir, "steps")
        def stepDir = new File(stepsDir, "writeMinecraftClasspathClient")
        def classpathFile = new File(stepDir, "classpath.txt")

        classpathFile.exists()

        classpathFile.text.contains("org.jgrapht${File.separator}jgrapht-core")
    }
}
