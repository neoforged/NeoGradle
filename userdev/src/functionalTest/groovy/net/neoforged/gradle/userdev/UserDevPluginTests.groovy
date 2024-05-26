package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification

class UserDevPluginTests extends SimpleTestSpecification {


    def "applying userdev plugin succeeds"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.userdev'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "applying userdev plugin applies neoform plugin"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.userdev'
            }
            
            println project.plugins
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('net.neoforged.gradle.neoform.NeoFormPlugin')
        result.output.contains('net.neoforged.gradle.common.CommonPlugin')
    }

    def "applying userdev plugin applies userDev runtime extension"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.userdev'
            }
            
            println project.userDevRuntime.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('UserDevRuntimeExtension')
    }
}
