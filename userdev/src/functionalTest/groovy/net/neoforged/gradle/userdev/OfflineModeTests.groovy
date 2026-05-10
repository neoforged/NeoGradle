package net.neoforged.gradle.userdev

import net.neoforged.gradle.common.services.caching.CachedExecutionService
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import org.gradle.internal.impldep.org.bouncycastle.util.BigIntegers
import org.gradle.testkit.runner.TaskOutcome
import net.neoforged.gradle.userdev.constants.TestConstants

class OfflineModeTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.userdev";
        injectIntoAllProject = true;
    }

    def "running_a_second_time_should_be_possible_when_offline"() {
        given:
        def project = create("running_second_when_offline", {
            it.withRun()
            it.retainBuildDirectoryBetweenRuns()
        })

        when:
        def run = project.run {
            it.run()
        }

        then:
        run.checkModLoading()

        when:
        project.resetSelfTestFile()
        def runOffline = project.run {
            it.offline()
            it.run()
        }

        then:
        runOffline.checkModLoading()
    }
}
