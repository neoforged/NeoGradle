package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ParchmentTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }

    def "parchment can be used"() {
        given:
        def project = create("parchment_can_be_used", {
            it.property("neogradle.subsystems.parchment.minecraftVersion", "1.21")
            it.property("neogradle.subsystems.parchment.mappingsVersion", "2024.07.28")
            it.build("""
            plugins {
                id 'net.neoforged.gradle.userdev'
            }

            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.file("src/main/java/net/neoforged/gradle/userdev/ConfigurationCacheTests.java", """
                package net.neoforged.gradle.userdev;
                
                import net.minecraft.client.Minecraft;
                
                public class ConfigurationCacheTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
            it.arguments('--warning-mode', 'fail', '--stacktrace')
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        run.task(':neoFormApplyParchment').outcome == TaskOutcome.SUCCESS
    }
}
