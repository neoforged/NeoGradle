package net.minecraftforge.gradle.userdev

import net.minecraftforge.gradle.base.ForgeGradleTestSpecification
import net.minecraftforge.gradle.common.util.Utils

class UserDevPluginTests extends ForgeGradleTestSpecification {


    def "applying userdev plugin succeeds"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
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
                id 'net.minecraftforge.gradle'
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
                id 'net.minecraftforge.gradle'
            }
            
            println project.forge.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('ForgeUserDevExtension')
    }

    def "applying userdev plugin applies userDev runtime extension"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle'
            }
            
            println project.forgeUserDevRuntime.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('ForgeUserDevRuntimeExtension')
    }
}
