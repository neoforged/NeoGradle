package net.minecraftforge.gradle.mcp

import net.minecraftforge.gradle.base.ForgeGradleTestSpecification
import net.minecraftforge.gradle.common.util.Utils

class McpPluginTests extends ForgeGradleTestSpecification {


    def "applying mcp plugin succeeds"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
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
                id 'net.minecraftforge.gradle.mcp'
            }
            
            println project.plugins
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('net.minecraftforge.gradle.common.CommonPlugin')
    }

    def "applying mcp plugin applies mcp extension"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
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
                id 'net.minecraftforge.gradle.mcp'
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
                id 'net.minecraftforge.gradle.mcp'
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
        result.output.contains(Utils.MOJANG_MAVEN)
        result.output.contains(Utils.FORGE_MAVEN)
    }
}
