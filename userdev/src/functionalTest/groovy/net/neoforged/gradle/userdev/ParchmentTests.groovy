package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome
import net.neoforged.gradle.userdev.constants.TestConstants

class ParchmentTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "parchment can be used"() {
        given:
        def project = create("parchment_can_be_used", {
            it.property("neogradle.subsystems.parchment.minecraftVersion", "1.21")
            it.property("neogradle.subsystems.parchment.mappingsVersion", "2024.07.28")
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(${TestConstants.Latest.JavaVersion})
            
            dependencies {
                implementation 'net.neoforged:neoforge:+'
            }
            """)
            it.javaClassFile("net.neoforged.gradle.userdev.ConfigurationCacheTests", """
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
        run.task(':neoFormTransformSource').outcome == TaskOutcome.SUCCESS
    }
}
