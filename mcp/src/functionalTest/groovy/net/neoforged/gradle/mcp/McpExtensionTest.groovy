package net.neoforged.gradle.mcp

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification

class McpExtensionTest extends SimpleTestSpecification {

    def "mcp extension test has convention for default artifact based on version"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            project.mcp.mcpConfigVersion.set '2017.2.39' //Random version. Doesn't matter.
            println project.mcp.mcpConfigArtifact.get()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('de.oceanlabs.mcp:mcp_config:2017.2.39@zip')
    }

    def "mcp extension test has convention which fails without version"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            println project.mcp.mcpConfigArtifact.get()
        """

        when:
        def result = gradleRunner().buildAndFail()

        then:
        result.output.contains('Cannot query the value of extension \'mcp\' property \'mcpConfigArtifact\' because it has no value available.')
        result.output.contains('- extension \'mcp\' property \'mcpConfigVersion\'')
    }
}
