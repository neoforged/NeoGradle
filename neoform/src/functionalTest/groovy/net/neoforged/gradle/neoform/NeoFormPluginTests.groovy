package net.neoforged.gradle.neoform

import net.neoforged.gradle.util.UrlConstants
import net.neoforged.trainingwheels.gradle.functional.BuilderBasedTestSpecification
import net.neoforged.trainingwheels.gradle.functional.builder.Runtime
import org.gradle.testkit.runner.TaskOutcome

class NeoFormPluginTests extends BuilderBasedTestSpecification {

    @Override
    protected void configurePluginUnderTest() {
        pluginUnderTest = "net.neoforged.gradle.neoform";
        injectIntoAllProject = true;
    }

    def "can apply plugin"() {
        given:
        def project = create "apply-neoform", {
        }

        when:
        def run = project.run {
            it.tasks 'tasks'
            it.log(Runtime.LogLevel.INFO)
            it.arguments('-s')
        }

        then:
        run.task(":tasks").outcome == TaskOutcome.SUCCESS;
    }

    def "applying neoform plugin applies common plugin"() {
        given:
        def project = create "apply-neoform-applies-common", {
            it.build("""
            tasks.register('plugins') {
                doLast {
                    project.getPlugins().forEach { plugin -> project.logger.lifecycle plugin.class.name }
                }
            }
            """)
        }

        when:
        def run = project.run { it.tasks 'plugins' }

        then:
        run.task(':plugins').outcome == TaskOutcome.SUCCESS
        run.output.contains('CommonProjectPlugin')
    }

    def "applying neoform plugin adds a neoFormRuntime configurable extension"() {
        given:
        def project = create "apply-neoform-allows-neoformruntime-configuration", {
            it.build("""
            neoFormRuntime { }
            """)
        }

        when:
        def run = project.run { it.tasks 'tasks' }

        then:
        run.task(":tasks").outcome == TaskOutcome.SUCCESS
    }

    def "applying neoform plugin adds the required maven plugins"() {
        given:
        def project = create "apply-neoform-adds-required-mavens", {
            it.build("""
            tasks.register('repositoryUrls') {
                doLast {
                    project.repositories.each { repo ->
                        println repo.url
                    }
                }
            }
            """)
        }

        when:
        def run = project.run { it.tasks "repositoryUrls" }

        then:
        run.task(":repositoryUrls").outcome == TaskOutcome.SUCCESS
        run.output.contains(UrlConstants.MOJANG_MAVEN)
        run.output.contains(UrlConstants.NEO_FORGE_MAVEN)
    }
}
