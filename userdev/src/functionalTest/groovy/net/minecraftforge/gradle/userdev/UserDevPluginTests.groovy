package net.minecraftforge.gradle.userdev

import net.minecraftforge.trainingwheels.gradle.functional.SimpleTestSpecification

class UserDevPluginTests extends SimpleTestSpecification {


    def "applying userdev plugin succeeds"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.userdev'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "applying userdev plugin applies mcp plugin"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.userdev'
            }
            
            println project.plugins
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('net.minecraftforge.gradle.mcp.McpPlugin')
        result.output.contains('net.minecraftforge.gradle.common.CommonPlugin')
    }

    def "applying userdev plugin applies forge extension"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.userdev'
            }
            
            println project.userDev.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('UserDevExtension')
    }

    def "applying userdev plugin applies userDev runtime extension"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.userdev'
            }
            
            println project.userDevRuntime.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('UserDevRuntimeExtension')
    }
}
