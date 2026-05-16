package net.neoforged.gradle.userdev.convention


import net.neoforged.gradle.userdev.constants.TestConstants
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification

/**
 * Tests the jarJar conventions.
 */
class JarJarConventionTests extends BuilderBasedTestSpecification {
    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "by default the main jarJar task and configuration are created"() {
        given:
        def project = create("jarjar_default_creates_main_feature", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }

            afterEvaluate {
                logger.lifecycle("JarJar task present: \${project.tasks.findByName('jarJar') != null}")
                logger.lifecycle("JarJar configuration present: \${project.configurations.findByName('jarJar') != null}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':help')
        }

        then:
        run.output.contains("JarJar task present: true")
        run.output.contains("JarJar configuration present: true")
    }

    def "disabling create-main-jarjar prevents the main jarJar task and configuration from being created"() {
        given:
        def project = create("jarjar_disable_main_feature", {
            it.property('neogradle.subsystems.conventions.jarjar.create-main-jarjar', 'false')
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }

            afterEvaluate {
                logger.lifecycle("JarJar task present: \${project.tasks.findByName('jarJar') != null}")
                logger.lifecycle("JarJar configuration present: \${project.configurations.findByName('jarJar') != null}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':help')
        }

        then:
        run.output.contains("JarJar task present: false")
        run.output.contains("JarJar configuration present: false")
    }

    def "disabling jarjar conventions globally prevents the main jarJar task and configuration from being created"() {
        given:
        def project = create("jarjar_disable_conventions_globally", {
            it.property('neogradle.subsystems.conventions.jarjar.enabled', 'false')
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }

            afterEvaluate {
                logger.lifecycle("JarJar task present: \${project.tasks.findByName('jarJar') != null}")
                logger.lifecycle("JarJar configuration present: \${project.configurations.findByName('jarJar') != null}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':help')
        }

        then:
        run.output.contains("JarJar task present: false")
        run.output.contains("JarJar configuration present: false")
    }

    def "with create-main-jarjar disabled the jarJar extension is still registered for custom features"() {
        given:
        def project = create("jarjar_extension_present_when_default_disabled", {
            it.property('neogradle.subsystems.conventions.jarjar.create-main-jarjar', 'false')
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
                }
            }

            repositories {
                mavenCentral()
            }

            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }

            afterEvaluate {
                logger.lifecycle("JarJar extension present: \${project.extensions.findByName('jarJar') != null}")
                logger.lifecycle("Default jarJar task present: \${project.tasks.findByName('jarJar') != null}")
                logger.lifecycle("Default jarJar configuration present: \${project.configurations.findByName('jarJar') != null}")
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':help')
        }

        then:
        run.output.contains("JarJar extension present: true")
        run.output.contains("Default jarJar task present: false")
        run.output.contains("Default jarJar configuration present: false")
    }
}