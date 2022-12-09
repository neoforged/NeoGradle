package net.minecraftforge.gradle.common.deobfuscation

import net.minecraftforge.gradle.base.ForgeGradleTestSpecification

class DependencyDeobfuscatorTestSpecification extends ForgeGradleTestSpecification {

    def "when a deobfuscatable dependency is added then it is replaced"() {
        given:
        settingsFile << "rootProject.name = 'mcp-plugin-apply-succeeds'"
        buildFile << """
            plugins {
                id 'net.minecraftforge.gradle.common'
            }
            
            repositories {
                maven {
                    name = "Forge"
                    url = "https://maven.minecraftforge.net/"
                }
            }  
                         
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
                        
            dependencies {
                implementation 'net.minecraftforge:artifactural:3.0.8'
            }
        """

        when:
        def result = gradleRunner()
                .withArguments('--stacktrace', ':build')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }

}
