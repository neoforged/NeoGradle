package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification

class SourceSetTests  extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "inheriting a sourceset adds the compile dependencies"() {
        given:
        def project = create("inheriting_sourcesets_compile", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            sourceSets {
                target {
                    java {
                        srcDir 'src/main/target'
                    }
                    
                    inherits.from main
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'com.google.code.gson:gson:2.11.0'
            }
            
            afterEvaluate {
                logger.lifecycle("Compile classpath: ")
                sourceSets.target.compileClasspath.files.each { file ->
                    logger.lifecycle("  - " + file.toString())
                }
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':tasks')
        }

        then:
        run.getOutput().contains("com.google.code.gson/gson/2.11.0") || run.getOutput().contains("com.google.code.gson\\gson\\2.11.0")
    }

    def "inheriting a sourceset adds the runtime dependencies"() {
        given:
        def project = create("inheriting_sourcesets_runtime", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            sourceSets {
                target {
                    java {
                        srcDir 'src/main/target'
                    }
                    
                    inherits.from main
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'com.google.code.gson:gson:+'
            }
            
            afterEvaluate {
                logger.lifecycle("Runtime classpath: ")
                sourceSets.target.runtimeClasspath.files.each { file ->
                    logger.lifecycle("  - " + file.toString())
                }
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':tasks')
        }

        then:
        run.getOutput().contains("com.google.code.gson/gson/2.11.0") || run.getOutput().contains("com.google.code.gson\\gson\\2.11.0")
        run.getOutput().contains("inheriting_sourcesets_runtime/build/classes/java/main") || run.getOutput().contains("inheriting_sourcesets_runtime\\build\\classes\\java\\main")
    }

    def "depending on a sourceset adds the compile dependencies"() {
        given:
        def project = create("depending_sourcesets_compile", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            sourceSets {
                target {
                    java {
                        srcDir 'src/main/target'
                    }
                    
                    depends.on main
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'com.google.code.gson:gson:2.11.0'
            }
            
            afterEvaluate {
                logger.lifecycle("Compile classpath: ")
                sourceSets.target.compileClasspath.files.each { file ->
                    logger.lifecycle("  - " + file.toString())
                }
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':tasks')
        }

        then:
        run.getOutput().contains("com.google.code.gson/gson/2.11.0") || run.getOutput().contains("com.google.code.gson\\gson\\2.11.0")
        run.getOutput().contains("depending_sourcesets_compile/build/classes/java/main") || run.getOutput().contains("depending_sourcesets_compile\\build\\classes\\java\\main")
    }

    def "depending on a sourceset adds the runtime dependencies"() {
        given:
        def project = create("depending_sourcesets_runtime", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            sourceSets {
                target {
                    java {
                        srcDir 'src/main/target'
                    }
                    
                    depends.on main
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                implementation 'com.google.code.gson:gson:2.11.0'
            }
            
            afterEvaluate {
                logger.lifecycle("Runtime classpath: ")
                sourceSets.target.runtimeClasspath.files.each { file ->
                    logger.lifecycle("  - " + file.toString())
                }
            }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks(':tasks')
        }

        then:
        run.getOutput().contains("com.google.code.gson/gson/2.11.0") || run.getOutput().contains("com.google.code.gson\\gson\\2.11.0")
        run.getOutput().contains("depending_sourcesets_runtime/build/classes/java/main") || run.getOutput().contains("depending_sourcesets_runtime\\build\\classes\\java\\main")
    }
}
