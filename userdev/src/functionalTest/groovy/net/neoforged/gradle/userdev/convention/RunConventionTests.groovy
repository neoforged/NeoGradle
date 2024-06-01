package net.neoforged.gradle.userdev.convention

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

/**
 * Tests the run conventions.
 * Note: When counting the elements in a NDOC do not use size(), values are not guaranteed to be present.
 */
class RunConventionTests extends BuilderBasedTestSpecification {
    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "disabling conventions globally does not register runs"() {
        given:
        def project = create("disable_globally_disables_registration", {
            it.property('neogradle.subsystems.conventions.enabled', 'false')
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
            
            afterEvaluate {
                logger.lifecycle("Run count: \${project.runs.stream().count()}")
                logger.lifecycle("Runtype count: \${project.runTypes.stream().count()}")
            }                        
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("Run count: 0")
        run.output.contains("Runtype count:")
        !run.output.contains("Runtype count: 0")
    }

    def "disabling run conventions does not register runs"() {
        given:
        def project = create("disable_runs_disables_registration", {
            it.property('neogradle.subsystems.conventions.runs.enabled', 'false')
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
            
            afterEvaluate {
                logger.lifecycle("Run count: \${project.runs.stream().count()}")
                logger.lifecycle("Runtype count: \${project.runTypes.stream().count()}")
            }                        
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("Run count: 0")
        run.output.contains("Runtype count:")
        !run.output.contains("Runtype count: 0")
    }

    def "disabling automatic registration does not register runs"() {
        given:
        def project = create("disable_automatic_registration_disables_registration", {
            it.property('neogradle.subsystems.conventions.runs.create-default-run-per-type', 'false')
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
            
            afterEvaluate {
                logger.lifecycle("Run count: \${project.runs.stream().count()}")
                logger.lifecycle("Runtype count: \${project.runTypes.stream().count()}")
            }                        
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("Run count: 0")
        run.output.contains("Runtype count:")
        !run.output.contains("Runtype count: 0")
    }

    def "enabling automatic registration does not register runs"() {
        given:
        def project = create("disable_automatic_registration_disables_registration", {
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
            
            afterEvaluate {
                // Do not use size here.
                logger.lifecycle("Run count: \${project.runs.stream().count()}")
                logger.lifecycle("Runtype count: \${project.runTypes.stream().count()}")
                logger.lifecycle("Equal: \${project.runTypes.stream().count() == project.runs.stream().count()}")
            }                        
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("Run count: ")
        !run.output.contains("Run count: 0")
        run.output.contains("Runtype count:")
        !run.output.contains("Runtype count: 0")
        run.output.contains("Equal: true")
    }

    def "disabling conventions globally prevents creation of runs configuration run"() {
        given:
        def project = create("disable_globally_errors_run", {
            it.property('neogradle.subsystems.conventions.enabled', 'false')
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
                clientRun 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method clientRun() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling conventions globally prevents creation of runs configuration runs"() {
        given:
        def project = create("disable_globally_errors_runs", {
            it.property('neogradle.subsystems.conventions.enabled', 'false')
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
                runs 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method runs() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "using the run convention configuration puts the dependency on the runtime config"() {
        given:
        def project = create("run_can_download_runtime_elements", {
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
                clientRun 'org.jgrapht:jgrapht-core:+'
            }
            
            afterEvaluate {
                logger.lifecycle("Run contains cp entry: \${project.runs.client.dependencies.get().runtimeConfiguration.files.any { it.name.contains 'jgrapht' }}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run contains cp entry: true")
    }

    def "using the runs convention configuration puts the dependency on the runtime config"() {
        given:
        def project = create("runs_can_download_runtime_elements", {
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
                runs 'org.jgrapht:jgrapht-core:+'
            }
            
            afterEvaluate {
                logger.lifecycle("Run contains cp entry: \${project.runs.client.dependencies.get().runtimeConfiguration.files.any { it.name.contains 'jgrapht' }}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run contains cp entry: true")
    }
}
