package net.neoforged.gradle.platform


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Files

class E2ETests extends BuilderBasedTestSpecification {

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

    private PublishingProjectSetup createPublishingProject(String projectId) {
        def rootProject = create(projectId, {
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

    def "default setup initializes"() {
        given:
        def project = createPublishingProject("default-setup")

        when:
        def rootRun = project.rootProject.run {
            it.tasks ':tasks'
            it.stacktrace()
        }

        then:
        rootRun.task(":tasks").outcome == TaskOutcome.SUCCESS

        when:
        def baseRun = project.rootProject.run {
            it.tasks ':base:tasks'
            it.stacktrace()
        }

        then:
        baseRun.task(":base:tasks").outcome == TaskOutcome.SUCCESS

        when:
        def patchedRun = project.rootProject.run {
            it.tasks ':neoforge:tasks'
            it.stacktrace()
        }

        then:
        patchedRun.task(":neoforge:tasks").outcome == TaskOutcome.SUCCESS
    }

    def "default setup can run setup on base project"() {
        given:
        def project = createPublishingProject("setup-base")

        when:
        def rootRun = project.rootProject.run {
            it.tasks ':base:setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":base:setup").outcome == TaskOutcome.SUCCESS
    }

    def "default setup can run setup on patched project"() {
        given:
        def project = createPublishingProject("setup-patched")

        when:
        def rootRun = project.rootProject.run {
            it.tasks ':neoforge:setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":neoforge:setup").outcome == TaskOutcome.SUCCESS
    }

    def "default setup can run setup globally"() {
        given:
        def project = createPublishingProject("setup-globally")

        when:
        def rootRun = project.rootProject.run {
            it.tasks 'setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":base:setup").outcome == TaskOutcome.SUCCESS
        rootRun.task(":neoforge:setup").outcome == TaskOutcome.SUCCESS
    }

    def "default setup can run setup globally, but does not requires double decompile"() {
        given:
        def project = createPublishingProject("setup-caching")

        when:
        def rootRun = project.rootProject.run {
            it.tasks 'setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":base:neoFormDecompile").outcome == TaskOutcome.SUCCESS || rootRun.task(":base:neoFormDecompile").outcome == TaskOutcome.UP_TO_DATE
        rootRun.task(":neoforge:neoFormDecompile").outcome == TaskOutcome.SUCCESS || rootRun.task(":neoforge:neoFormDecompile").outcome == TaskOutcome.UP_TO_DATE

        (rootRun.task(":base:neoFormDecompile").outcome == TaskOutcome.SUCCESS && rootRun.task(":neoforge:neoFormDecompile").outcome == TaskOutcome.UP_TO_DATE) ||
                (rootRun.task(":base:neoFormDecompile").outcome == TaskOutcome.UP_TO_DATE && rootRun.task(":neoforge:neoFormDecompile").outcome == TaskOutcome.SUCCESS)
    }

    def "patching and building is possible"() {
        given:
        def project = createPublishingProject("patching-assemble")

        when:
        def rootRun = project.rootProject.run {
            it.tasks ':neoforge:setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":neoforge:setup").outcome == TaskOutcome.SUCCESS

        when:
        patch(project)

        then:
        project.patchedProject.file(PATCH_TARGET_PATH).text.contains("This is a comment inserted by the test")

        when:
        def createdPatchesRun = project.rootProject.run {
            it.tasks ':neoforge:unpackSourcePatches'
            it.stacktrace()
        }

        then:
        createdPatchesRun.task(":neoforge:unpackSourcePatches").outcome == TaskOutcome.SUCCESS

        when:
        def patchedRun = project.rootProject.run {
            it.tasks ':neoforge:assemble'
            it.stacktrace()
        }

        then:
        patchedRun.task(":neoforge:assemble").outcome == TaskOutcome.SUCCESS
    }

    def "userdev contains patch"() {
        given:
        def project = createPublishingProject("userdev-with-patch")

        when:
        def rootRun = project.rootProject.run {
            it.tasks ':neoforge:setup'
            it.stacktrace()
        }

        then:
        rootRun.task(":neoforge:setup").outcome == TaskOutcome.SUCCESS

        when:
        patch(project)

        then:
        project.patchedProject.file(PATCH_TARGET_PATH).text.contains("This is a comment inserted by the test")

        when:
        def createdPatchesRun = project.rootProject.run {
            it.tasks ':neoforge:unpackSourcePatches'
            it.stacktrace()
        }

        then:
        createdPatchesRun.task(":neoforge:unpackSourcePatches").outcome == TaskOutcome.SUCCESS

        when:
        def patchedRun = project.rootProject.run {
            it.tasks ':neoforge:assemble'
            it.stacktrace()
        }

        then:
        patchedRun.task(":neoforge:assemble").outcome == TaskOutcome.SUCCESS
        patchedRun.task(":neoforge:userdevJar").outcome == TaskOutcome.SUCCESS

        def userdevJar = project.patchedProject.file("build/libs/neoforge-1.0.0-userdev-with-patch-userdev.jar")
        userdevJar.exists()

        def patch = userdevJar.getZipEntry(PATCH_RESULT_PATH)
        patch.exists()

        def patchContent = patch.text
        patchContent.contains("This is a comment inserted by the test")
    }

    def "a published userdev artifact can be loaded into userdev"() {
        given:
        def project = createPublishingProject("published-userdev")
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
        def userdevProject = create("published-userdev-cons", {
            it.plugin("net.neoforged.gradle.userdev")
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                maven {
                    name = 'test'
                    url = "file://${getTestTempDirectory().absolutePath.replace(File.separator, "/")}/maven"
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:1.0.0-published-userdev'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = userdevProject.run {
            it.tasks(':neoFormRecompile')
        }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }
}
