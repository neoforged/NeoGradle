package net.neoforged.gradle.neoform.dependency

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification

class NeoformDependencyManagerTest extends SimpleTestSpecification {

    def "adding a dependency to something other then neoform minecraft works"() {
        given:
        settingsFile << "rootProject.name = 'neoform-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.neoform'
            }
            
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            dependencies {
                implementation 'com.google.guava:guava:30.1.1-jre'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "adding a dependency on neoform minecraft with exact version works"() {
        given:
        settingsFile << "rootProject.name = 'neoform-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.neoform'
            }
            
            java.toolchain.languageVersion = JavaLanguageVersion.of(21)
            
            dependencies {
                implementation 'net.minecraft:neoform_joined:+'
            }
        """

        when:
        def result = gradleRunner().withDebug(true).withArguments("--stacktrace").build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }
}
