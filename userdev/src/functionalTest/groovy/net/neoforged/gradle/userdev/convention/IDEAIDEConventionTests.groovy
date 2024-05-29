package net.neoforged.gradle.userdev.convention

import net.neoforged.gradle.common.caching.CentralCacheService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification

class IDEAIDEConventionTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "disabling conventions globally forces auto detection to be false"() {
        given:
        def project = create("disable_globally_disables_auto_detection", {
            it.property('neogradle.subsystems.conventions.enabled', 'false')
            //Force NeoGradle to think build with IDEA is enabled.
            it.file(".idea/gradle.xml", """
            <option name=\"delegatedBuild\" value=\"false\" />
            """)
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
            it.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, new File(tempDir, ".caches-global").getAbsolutePath())
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: false")
    }

    def "disabling ide conventions forces auto detection to be false"() {
        given:
        def project = create("disable_ide_disables_auto_detection", {
            it.property('neogradle.subsystems.conventions.ide.enabled', 'false')
            //Force NeoGradle to think build with IDEA is enabled.
            it.file(".idea/gradle.xml", """
            <option name=\"delegatedBuild\" value=\"false\" />
            """)
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
            it.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, new File(tempDir, ".caches-global").getAbsolutePath())
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: false")
    }

    def "disabling idea ide conventions forces auto detection to be false"() {
        given:
        def project = create("disable_idea_ide_disables_auto_detection", {
            it.property('neogradle.subsystems.conventions.ide.idea.enabled', 'false')
            //Force NeoGradle to think build with IDEA is enabled.
            it.file(".idea/gradle.xml", """
            <option name=\"delegatedBuild\" value=\"false\" />
            """)
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
            it.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, new File(tempDir, ".caches-global").getAbsolutePath())
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: false")
    }

    def "disabling compiler auto-detection conventions forces auto detection to be false"() {
        given:
        def project = create("disable_compiler_idea_ide_disables_auto_detection", {
            it.property('neogradle.subsystems.conventions.ide.idea.compiler-detection', 'false')
            //Force NeoGradle to think build with IDEA is enabled.
            it.file(".idea/gradle.xml", """
            <option name=\"delegatedBuild\" value=\"false\" />
            """)
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
            it.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, new File(tempDir, ".caches-global").getAbsolutePath())
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: false")
    }

    def "enabling compiler auto-detection conventions detects idea compiler"() {
        given:
        def project = create("disable_compiler_idea_ide_disables_auto_detection", {
            //Force NeoGradle to think build with IDEA is enabled.
            it.file(".idea/gradle.xml", """
            <option name=\"delegatedBuild\" value=\"false\" />
            """)
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
            it.property(CentralCacheService.CACHE_DIRECTORY_PROPERTY, new File(tempDir, ".caches-global").getAbsolutePath())
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: true")
    }

    def "enabling compiler auto-detection conventions detects gradle compiler"() {
        given:
        def project = create("disable_compiler_idea_ide_disables_auto_detection", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            afterEvaluate {
                logger.lifecycle("IDEA Run with IDEA: \${project.idea.project.runs.runWithIdea.get()}")
            }                        
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':dependencies')
        }

        then:
        run.output.contains("IDEA Run with IDEA: false")
    }
}
