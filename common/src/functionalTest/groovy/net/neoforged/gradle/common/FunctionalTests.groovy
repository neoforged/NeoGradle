package net.neoforged.gradle.common


import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.testkit.runner.TaskOutcome

class FunctionalTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.common";
        injectIntoAllProject = true;
    }

    def "a mod with common as dependency can run build with the client-extra dependencies"() {
        given:
        def project = create "common-can-run-clean-build", {
            it.build("""
            java.toolchain.languageVersion = JavaLanguageVersion.of(25)
            
            dependencies {
                implementation "net.minecraft:client:+:client-extra"
            }
            """)
            it.withToolchains()
            it.withGlobalCacheDirectory(tempDir)
        }

        when:
        def run = project.run {
            it.tasks(':clean', ':build')
            it.stacktrace()
            it.debug()
        }

        then:
        run.task(':clean').outcome == TaskOutcome.SUCCESS || run.task(':clean').outcome == TaskOutcome.UP_TO_DATE
        run.task(':build').outcome == TaskOutcome.SUCCESS
    }
}
