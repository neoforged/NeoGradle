package net.neoforged.gradle.userdev.convention

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class SourceSetConventionTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "disabling conventions globally prevents creation of sourceset configuration localRuntime"() {
        given:
        def project = create("disable_globally_errors_local_runtime", {
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
                localRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling conventions globally prevents creation of sourceset configuration localRunRuntime"() {
        given:
        def project = create("disable_globally_errors_local_run_runtime", {
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRunRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling configuration conventions prevents creation of sourceset configuration localRuntime"() {
        given:
        def project = create("disable_configurations_errors_local_runtime", {
            it.property('neogradle.subsystems.conventions.configurations.enabled', 'false')
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
                localRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling configuration conventions prevents creation of sourceset configuration localRunRuntime"() {
        given:
        def project = create("disable_configurations_errors_local_run_runtime", {
            it.property('neogradle.subsystems.conventions.configurations.enabled', 'false')
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRunRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling sourceset conventions prevents creation of sourceset configuration localRuntime"() {
        given:
        def project = create("disable_sourcesets_fails_local_runtime", {
            it.property('neogradle.subsystems.conventions.sourcesets.enabled', 'false')
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
                localRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling sourceset conventions prevents creation of sourceset configuration localRunRuntime"() {
        given:
        def project = create("disable_sourcesets_fails_local_run_runtime", {
            it.property('neogradle.subsystems.conventions.sourcesets.enabled', 'false')
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
            it.shouldFail()
        }

        then:
        run.output.contains("Could not find method localRunRuntime() for arguments [org.jgrapht:jgrapht-core:+] on object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyHandler.")
    }

    def "disabling global conventions prevents registration of main sourceset to run"() {
        given:
        def project = create("disable_global_no_registration_main", {
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
            
            runs {
                client { }
            }
            
            afterEvaluate {
                logger.lifecycle("Run sources: \${project.runs.client.modSources.get()}")
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run sources: []")
    }

    def "disabling sourceset conventions prevents registration of main sourceset to run"() {
        given:
        def project = create("disable_sourcesets_no_registration_main", {
            it.property('neogradle.subsystems.conventions.sourcesets.enabled', 'false')
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
                client { }
            }
            
            afterEvaluate {
                logger.lifecycle("Run sources: \${project.runs.client.modSources.get()}")
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run sources: []")
    }

    def "disabling main source set registration conventions prevents registration of main sourceset to run"() {
        given:
        def project = create("disable_sourcesets_no_registration_main", {
            it.property('neogradle.subsystems.conventions.sourcesets.automatic-inclusion', 'false')
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
                client { }
            }
            
            afterEvaluate {
                logger.lifecycle("Run sources: \${project.runs.client.modSources.get()}")
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run sources: []")
    }

    def "having the conventions for main sourceset registration enabled registers it"() {
        given:
        def project = create("registration_enabled", {
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
                client { }
            }
            
            afterEvaluate {
                logger.lifecycle("Run sources: \${project.runs.client.modSources.get()}")
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run sources: [source set 'main']")
    }

    def "disabling sourceset local run runtime registration conventions prevents registration of localRunRuntime"() {
        given:
        def project = create("disable_sourcesets_prevents_local_run_runtime", {
            it.property('neogradle.subsystems.conventions.sourcesets.automatic-inclusion-local-run-runtime', 'false')
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            
            runs {
                client { }
            }
            """)
            it.withToolchains()
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

        !classpathFile.text.contains("org.jgrapht/jgrapht-core")
    }

    def "enabling sourceset local run runtime registration conventions registers localRunRuntime"() {
        given:
        def project = create("disable_sourcesets_prevents_local_run_runtime", {
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            
            runs {
                client { }
            }
            """)
            it.withToolchains()
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

        classpathFile.text.contains("org.jgrapht/jgrapht-core")
    }

    def "using the local runtime convention configuration does not put the dependency on the runtime config"() {
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
                localRuntime 'org.jgrapht:jgrapht-core:+'
            }
            
            afterEvaluate {
                logger.lifecycle("Run contains cp entry: \${project.runs.client.dependencies.get().runtimeConfiguration.files.any { it.name.contains 'jgrapht' }}")
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains("Run contains cp entry: false")
    }

    def "using the local run runtime convention configuration does not put the dependency on the runtime config"() {
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
                localRunRuntime 'org.jgrapht:jgrapht-core:+'
            }
            
            afterEvaluate {
                logger.lifecycle("Run contains cp entry: \${project.runs.client.dependencies.get().runtimeConfiguration.files.any { it.name.contains 'jgrapht' }}")
            }
            """)
            it.withToolchains()
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
