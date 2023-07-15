package net.neoforged.gradle.mcp


import net.minecraftforge.trainingwheels.gradle.functional.SimpleTestSpecification
import net.neoforged.gradle.util.UrlConstants
import org.gradle.testkit.runner.TaskOutcome

class McpPluginTests extends SimpleTestSpecification {

    def "applying mcp plugin succeeds"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "applying mcp plugin applies common plugin"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            println project.plugins
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('net.neoforged.gradle.common.CommonPlugin')
    }

    def "applying mcp plugin applies mcp extension"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            println project.mcp.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('McpExtension')
    }

    def "applying mcp plugin applies mcp runtime extension"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            println project.mcpRuntime.class.toString()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('McpRuntimeExtension')
    }

    def "applying mcp plugin registers mojang and forge maven"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            project.afterEvaluate {
                project.repositories.each { repo ->
                    println repo.url
                }
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains(UrlConstants.MOJANG_MAVEN)
        result.output.contains(UrlConstants.NEO_FORGE_MAVEN)
        result.output.contains(UrlConstants.MCF_FORGE_MAVEN)
    }

    def "applying mcp plugin registers display mappings task"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
        """

        when:
        def result = gradleRunner().withArguments('handleNamingLicense').build()

        then:
        result.task(':handleNamingLicense').outcome == TaskOutcome.SUCCESS
    }
}
