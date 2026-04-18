package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class ConfigurationCacheTests extends BuilderBasedTestSpecification {

    private static final String NEOFORM_VERSION = "26.1-snapshot-1-3"

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }

    def "compile_supports_configuration_cache_build"() {
        given:
        def project = create("compile_supports_configuration_cache_build", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(25)
            
            dependencies {
                implementation 'net.minecraft:neoform_joined:${NEOFORM_VERSION}'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
        }

        then:
        run.task(':compileJava').outcome == TaskOutcome.NO_SOURCE
    }

    def "compile_supports_configuration_cache_build_and_is_reused"() {
        given:
        def project = create("compile_supports_configuration_cache_build_and_is_reused", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(25)
            
            dependencies {
                implementation 'net.minecraft:neoform_joined:${NEOFORM_VERSION}'
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
            it.enableLocalBuildCache()
            it.enableConfigurationCache()
        })

        when:
        def run = project.run {
            it.tasks('compileJava')
        }

        and:
        def secondaryRun = project.run {
            it.tasks('compileJava')
        }

        and:
        def thirdRun = project.run {
            it.tasks('compileJava')
        }

        then:
        thirdRun.output.contains('Reusing configuration cache.')
        run.task(':neoFormDecompile').outcome == TaskOutcome.SUCCESS
        run.task(':compileJava').outcome == TaskOutcome.SUCCESS
        thirdRun.task(':neoFormDecompile').outcome == TaskOutcome.FROM_CACHE
        thirdRun.task(':compileJava').outcome == TaskOutcome.FROM_CACHE
    }
}
