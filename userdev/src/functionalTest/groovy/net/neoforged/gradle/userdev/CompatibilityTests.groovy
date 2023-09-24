package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification
import spock.lang.Ignore

@Ignore
class CompatibilityTests extends SimpleTestSpecification {

    def "a mod with userdev as dependency can run the patch task for that dependency"() {
        given:
        settingsFile << """
        rootProject.name = 'test-project'
        plugins {
            id 'org.gradle.toolchains.foojay-resolver-convention' version '0.4.0'
        }
        """
        buildFile << """
            plugins {
                id 'net.neoforged.gradle.userdev'
            }
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
        """

        when:
        def result = gradleRunner()
                .withArguments('--stacktrace', 'neoFormRecompile')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }
}
