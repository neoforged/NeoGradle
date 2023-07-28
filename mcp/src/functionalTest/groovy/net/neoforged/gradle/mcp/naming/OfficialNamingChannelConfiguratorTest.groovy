package net.neoforged.gradle.mcp.naming

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification

class OfficialNamingChannelConfiguratorTest extends SimpleTestSpecification {

    def "registers the naming official naming channel as the default"() {
        given:
        settingsFile << "rootProject.name = 'official-naming-channel-configurator'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            println project.minecraft.mappings.channel.get().getName()
        """

        when:
        def result = gradleRunner().build()

        then:
        result.output.contains('official')
    }

    def "official naming channel emits no license text warning when no runtime are setup"() {
       given:
        settingsFile << "rootProject.name = 'official-naming-channel-configurator'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            minecraft {
                mappings {
                    channel = official()
                }
            }
        """

        when:
        def result = gradleRunner().withArguments("build").build()

        then:
        result.output.contains('No license text found')
    }

    def "official naming channel emits license text when a dependency is setup"() {
        given:
        settingsFile << "rootProject.name = 'official-naming-channel-configurator'"
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.mcp'
            }
            
            minecraft {
                mappings {
                    channel = official()
                }
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.minecraft:mcp_client:1.19.2-20220805.130853'
            }
        """

        when:
        def result = gradleRunner().withArguments("build").build()

        then:
        result.output.contains('These mappings are provided "as-is" and you bear the risk of using them.')
    }
}
