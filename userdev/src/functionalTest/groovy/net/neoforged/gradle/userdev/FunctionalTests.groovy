package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends BuilderBasedTestSpecification {

    private static final String NEOFORGE_VERSION = "20.2.43-beta"

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev"
        injectIntoAllProject = true
    }

    def "a mod with userdev: as dependency can run the recompile task for that dependency"() {
        given:
        def project = create "test-project", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:${NEOFORGE_VERSION}'
            }
            """)
            it.withToolchains()
        }

        when:
        def run = project.run { it.log(Runtime.LogLevel.INFO).tasks(':neoFormRecompile') }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }

    def "a mod with userdev as dependency and official mappings can compile through gradle"() {
        given:
        def project = create "test-project", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:${NEOFORGE_VERSION}'
            }
            """)
            it.withToolchains()

            it.file("src/main/java/net/minecraftforge/gradle/userdev/FunctionalTests.java", """
                package net.neoforged.gradle.mcp;
                
                import net.minecraft.client.Minecraft;
                
                public class FunctionalTests {
                    public static void main(String[] args) {
                        System.out.println(Minecraft.getInstance().getClass().toString());
                    }
                }
            """)
        }

        when:
        def run = project.run { it.tasks('build') }

        then:
        run.output.output.contains('BUILD SUCCESSFUL')
    }

    def "the userdev runtime by default supports the build cache"() {
        given:
        def project = create "test-project", {
            it.build("""
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(17)
                }
            }
            
            dependencies {
                implementation 'net.neoforged:neoforge:${NEOFORGE_VERSION}'
            }
            """)
            it.withToolchains()
            it.enableLocalBuildCache()
        }

        when:
        def run = project.run { it.arguments("--build-cache").tasks('build') }

        then:
        run.task(":neoFormRecompile").outcome == TaskOutcome.SUCCESS

        when:
        new File(project.projectDir, 'build').deleteDir()
        run = project.run { it.arguments("--build-cache").tasks('build') }

        then:
        run.task(":neoFormRecompile").outcome == TaskOutcome.FROM_CACHE
    }
}
