package net.neoforged.gradle.platform

import net.neoforged.gradle.common.services.caching.CachedExecutionService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Files

class InstallerProfileTests  extends BuilderBasedTestSpecification {

    private static final String TEST_NEOFORM_VERSION = "1.21-20240613.152323"

    private static final String PATCH_TARGET_PATH = "src/main/java/net/minecraft/client/Minecraft.java"
    private static final String PATCH_RESULT_PATH = "patches/net/minecraft/client/Minecraft.java.patch"
    private static final String ICON_PATH = "docs/assets/neoforged.ico"
    private static final String INSTALLER_LOGO_PATH = "src/main/resources/neoforged_logo.png"
    private static final String INSTALLER_URL_LOGO_PATH = "src/main/resources/url.png"

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.platform"
        injectIntoAllProject = false
        injectIntoRootProject = false
    }

    private final class PublishingProjectSetup {
        private Runtime rootProject
        private Runtime baseProject
        private Runtime patchedProject
    }

    private PublishingProjectSetup createPublishingProject(String projectId, String patchedBuildConfiguration) {
        def rootProject = create(projectId, {
            it.property(CachedExecutionService.DEBUG_CACHE_PROPERTY, 'true')
            it.property("neogradle.runtime.platform.installer.debug", "true")
            it.settingsPlugin(pluginUnderTest)
            it.settings("""
                dynamicProjects {
                    include ':base'
                    include ':neoforge'
                
                    project(":base").projectDir = file("projects/base")
                    project(":neoforge").projectDir = file("projects/neoforge")
                }
            """.stripMargin())
            it.build("""
                group = 'net.neoforged'
                version = "1.0.0-${projectId}"

                allprojects {
                    version rootProject.version
                    group 'net.neoforged'
                    repositories {
                        mavenLocal()
                    }
                }
                
                subprojects {
                    apply plugin: 'java'
                
                    java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))
                }
            """)
            it.file("server_files/args.txt", """
                Something to Inject into            
            """)
            it.file("server_files/run.sh", """
                #!/bin/bash
                echo "Test server starter script"
            """)
            it.file("server_files/run.bat", """
                echo "Test server starter script"
            """)
            it.file("server_files/user_jvm_args.txt", """
                -Xmx2G
            """)
            //The following properties are needed as we do not have an abstract layer over the tokens needed.
            it.property("fancy_mod_loader_version", "1.0.0")
            it.enableGradleParallelRunning()
            it.enableConfigurationCache()
            it.enableLocalBuildCache()
            it.withGlobalCacheDirectory(tempDir)
        })

        def baseProject = create("${projectId}/projects/base", {
            it.build("""
                dynamicProject {
                    neoform("${TEST_NEOFORM_VERSION}")
                }
            """)
        })

        //TODO: We need better handling for neoforged dependency detection. Right now this is limited to this exact ga: net.neoforged:neoforge so we are limited to that setup
        def patchedProject = create("${projectId}/projects/neoforge", {
            it.plugin("maven-publish")
            it.build("""
                dynamicProject {
                    runtime("${TEST_NEOFORM_VERSION}",
                            rootProject.layout.projectDirectory.dir('patches'),
                            rootProject.layout.projectDirectory.dir('rejects'))
                }
                
                installerProfile {
                    profile = 'NeoGradle-Tests'
                }
                
                minecraft {
                    modIdentifier 'minecraft'
                }
                
                ${patchedBuildConfiguration}
                
                sourceSets {
                    main {
                        java {
                            srcDirs rootProject.file('src/main/java')
                        }
                        resources {
                            srcDirs rootProject.file('src/main/resources'), rootProject.file('src/generated/resources')
                        }
                    }
                }
                
                AdhocComponentWithVariants javaComponent = (AdhocComponentWithVariants) project.components.findByName("java")
                // Ensure the two default variants are not published, since they
                // contain Minecraft classes
                javaComponent.withVariantsFromConfiguration(configurations.apiElements) {
                    it.skip()
                }
                javaComponent.withVariantsFromConfiguration(configurations.runtimeElements) {
                    it.skip()
                }
                
                //TODO: Move these to platform so that they can be centrally round tripped tested.
                configurations {
                    modDevBundle {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, "data"))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-moddev-bundle:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {} // Publish it
                    }
                    modDevConfig {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, "data"))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-moddev-config:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {} // Publish it
                    }
                    installerJar {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EMBEDDED))
                            // The installer targets JDK 8
                            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-installer:" + project.version)
                        }
                        // Publish it
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                    universalJar {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, JavaVersion.current().majorVersion.toInteger())
                            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements, LibraryElements.JAR))
                        }
                        // Publish it
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                    modDevApiElements {
                        canBeResolved = false
                        canBeConsumed = true
                        afterEvaluate {
                            extendsFrom userdevCompileOnly, installerLibraries, moduleOnly
                        }
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_API))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-dependencies:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                    modDevRuntimeElements {
                        canBeResolved = false
                        canBeConsumed = true
                        afterEvaluate {
                            extendsFrom installerLibraries, moduleOnly
                        }
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-dependencies:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                    modDevModulePath {
                        canBeResolved = false
                        canBeConsumed = true
                        afterEvaluate {
                            extendsFrom moduleOnly
                        }
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-moddev-module-path:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                    modDevTestFixtures {
                        canBeResolved = false
                        canBeConsumed = true
                        attributes {
                            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
                        }
                        outgoing {
                            capability("net.neoforged:neoforge-moddev-test-fixtures:" + project.version)
                        }
                        javaComponent.addVariantsFromConfiguration(it) {}
                    }
                }

                dependencies {
                    modDevBundle("net.neoforged:neoform:${TEST_NEOFORM_VERSION}") {
                        capabilities {
                            requireCapability 'net.neoforged:neoform'
                        }
                        endorseStrictVersions()
                    }
                    modDevApiElements("net.neoforged:neoform:${TEST_NEOFORM_VERSION}") {
                        capabilities {
                            requireCapability 'net.neoforged:neoform-dependencies'
                        }
                        endorseStrictVersions()
                    }
                    modDevRuntimeElements("net.neoforged:neoform:${TEST_NEOFORM_VERSION}") {
                        capabilities {
                            requireCapability 'net.neoforged:neoform-dependencies'
                        }
                        endorseStrictVersions()
                    }
                }
                
                afterEvaluate {
                    artifacts {
                        modDevBundle(userdevJar) {
                            setClassifier("userdev") // Legacy
                        }
                        modDevConfig(createUserdevJson.output) {
                            builtBy(createUserdevJson)
                            setClassifier("moddev-config")
                        }
                        universalJar(signUniversalJar.output) {
                            builtBy(signUniversalJar)
                            setClassifier("universal")
                        }
                        installerJar(signInstallerJar.output) {
                            builtBy(signInstallerJar)
                            setClassifier("installer")
                        }
                    }
                }
                
                publishing {
                    publications {
                        maven(MavenPublication) {
                            groupId = project.group
                            artifactId = project.name
                            version = project.version
                    
                            from components.java
                    
                            versionMapping {
                                usage('java-api') {
                                    fromResolutionOf('runtimeClasspath')
                                }
                                usage('java-runtime') {
                                    fromResolutionResult()
                                }
                            }
                        }
                    }
                    repositories {
                        maven {
                            name = 'test'
                            url = "file://${getTestTempDirectory().absolutePath.replace(File.separator, "/")}/maven"
                        }
                    }
                }
            """)
        })

        def iconPath = rootProject.file(ICON_PATH)
        iconPath.parentFile.mkdirs()
        Files.copy(new File("src/functionalTest/resources/icon.ico").toPath(), iconPath.toPath())

        def installerLogoPath = rootProject.file(INSTALLER_LOGO_PATH)
        installerLogoPath.parentFile.mkdirs()
        Files.copy(new File("src/functionalTest/resources/icon.png").toPath(), installerLogoPath.toPath())

        def installerUrlLogoPath = rootProject.file(INSTALLER_URL_LOGO_PATH)
        installerUrlLogoPath.parentFile.mkdirs()
        Files.copy(new File("src/functionalTest/resources/icon.png").toPath(), installerUrlLogoPath.toPath())

        def result = new PublishingProjectSetup()
        result.rootProject = rootProject
        result.baseProject = baseProject
        result.patchedProject = patchedProject

        return result
    }

    private void patch(PublishingProjectSetup setup) {
        def minecraftClassSourceFile = setup.patchedProject.file(PATCH_TARGET_PATH)
        def minecraftClassContent = new ArrayList<>(minecraftClassSourceFile.readLines())
        def minecraftClassContentIndex = minecraftClassContent.findIndexOf { String line -> line.startsWith("public class Minecraft") }
        def insertedComment = "    // This is a comment inserted by the test"
        minecraftClassContent.add(minecraftClassContentIndex, insertedComment) //Insert the comment before the class statement

        minecraftClassSourceFile.delete()
        minecraftClassSourceFile.write(minecraftClassContent.join("\n"))
    }

    def "a published installer artifact has properly reconfigured tooling"() {
        given:
        def project = createPublishingProject("published-userdev", """
            subsystems {
               tools {
                  binaryPatcher = "net.neoforged.installertools:binarypatcher:2.1.5:fatjar"
               }
            }
        """)

        project.rootProject.run { it.tasks ':neoforge:setup' }
        patch(project)
        project.rootProject.run { it.tasks ':neoforge:unpackSourcePatches'}
        project.rootProject.run { it.tasks ':neoforge:assemble' }

        when:
        def publishingRun = project.rootProject.run {
            it.tasks ':neoforge:publishAllPublicationsToTestRepository'
        }

        then:
        publishingRun.task(":neoforge:publishAllPublicationsToTestRepository").outcome == TaskOutcome.SUCCESS

        and:
        def buildPlatformDir = project.patchedProject.file("build/platform")
        def platformSpecDir = buildPlatformDir.listFiles()[0];
        def createLegacyInstaller = new File(platformSpecDir, "steps/createLegacyInstallerJson")
        def jsonFile = new File(createLegacyInstaller, "install_profile.json")

        then:
        jsonFile.exists()
        jsonFile.text.contains("net.neoforged.installertools:binarypatcher:2.1.5:fatjar")
        !jsonFile.text.contains("net.neoforged.installertools:binarypatcher:2.1.7:fatjar")
    }

    def "a published installer can be invoked to install a server"() {
        given:
        def project = createPublishingProject("published-userdev", """
            tasks.register("installTestServer", JavaExec.class) {
                classpath(tasks.named("signInstallerJar").flatMap { it.output })
                args("--installServer", file("build/testserverinstall").absolutePath)
                dependsOn("signInstallerJar")
            }
        """)

        project.rootProject.run { it.tasks ':neoforge:setup' }
        patch(project)
        project.rootProject.run { it.tasks ':neoforge:unpackSourcePatches'}
        project.rootProject.run { it.tasks ':neoforge:assemble' }

        when:
        def publishingRun = project.rootProject.run {
            it.tasks ':neoforge:installTestServer'
        }

        then:
        publishingRun.task(":neoforge:installTestServer").outcome == TaskOutcome.SUCCESS

        and:
        def testServerDir = project.patchedProject.file("build/testserverinstall")

        then:
        testServerDir.exists()
        testServerDir.listFiles().size() > 0
    }
}

