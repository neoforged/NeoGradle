package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.SimpleTestSpecification

class CompatibilityTests extends SimpleTestSpecification {

    def "a mod with userdev as dependency can run the patch task for that dependency"() {
        given:
        settingsFile << "rootProject.name = 'test-project'"
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
                implementation 'net.neoforged:forge:1.20.1-47.1.56'
            }
        """

        when:
        def result = gradleRunner()
                .withArguments('--stacktrace', 'dependencyForge1.20.1-47.1.56Recompile')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
    }
}
