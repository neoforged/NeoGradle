package net.minecraftforge.gradle.mcp.dependency

import net.minecraftforge.trainingwheels.gradle.functional.SimpleTestSpecification

class McpDependencyManagerTest extends SimpleTestSpecification {

    def "adding a dependency to something other then mcp minecraft works"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
            }
            
            dependencies {
                implementation 'com.google.guava:guava:30.1.1-jre'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "adding a dependency on mcp minecraft with exact version works"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client:1.19-20220627.091056'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "adding a dependency on mcp minecraft without version takes extension default"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
            }
            
            mcp {
                mcpConfigVersion = '1.19-20220627.091056'
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "adding a dependency on mcp minecraft with minecraft version takes extension default"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
            }
            
            mcp {
                mcpConfigVersion = '20220627.091056'
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client:1.19'
            }
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

    def "adding a dependency on mcp minecraft with an invalid version fails"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.mcp'
            }
            
            mcp {
                mcpConfigVersion.set blah
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client'
            }
        """

        when:
        def result = gradleRunner().buildAndFail()

        then:
        result.output.contains('FAILURE: Build failed')
    }
}
