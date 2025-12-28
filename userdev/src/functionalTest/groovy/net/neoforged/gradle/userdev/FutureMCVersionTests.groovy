package net.neoforged.gradle.userdev

import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FutureMCVersionTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "a mod with userdev setup for 26.1 can run the recompile task"() {
        given:
        def project = create("running_patch_task_is_possible", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(25)
            
            repositories {
                mavenLocal()
            }
            
            repositories {
                maven {
                    name = "Maven for PR #2879" // https://github.com/neoforged/NeoForge/pull/2879
                    url = uri("https://prmaven.neoforged.net/NeoForge/pr2879")
                    content {
                        includeModule("net.neoforged", "neoforge")
                        includeModule("net.neoforged", "testframework")
                    }
                }
            }
                        
            dependencies {
                implementation 'net.neoforged:neoforge:26.1.0.0-alpha.3+snapshot-1'
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        })

        when:
        def run = project.run {
            it.tasks(':neoFormRecompile')
            it.stacktrace()
        }

        then:
        run.task(':neoFormRecompile').outcome == TaskOutcome.SUCCESS
    }


}
