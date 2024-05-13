package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "a mod with userdev in the implementation configuration does not leak it"() {
        given:
        def project = create("userdev_in_implementation_does_not_leak", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            
            runs {
                client {
                    modSources.add(project.getSourceSets().main)
                }
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
        })

        when:
        def run = project.run {
            it.tasks('dependencies')
        }

        then:
        run.task(':dependencies').outcome == TaskOutcome.SUCCESS
        run.output.contains(
'''
implementation - Implementation dependencies for the 'main' feature. (n)
No dependencies
'''
        )
    }
}
