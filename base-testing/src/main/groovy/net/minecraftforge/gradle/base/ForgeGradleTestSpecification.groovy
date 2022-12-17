package net.minecraftforge.gradle.base

import com.google.common.collect.Lists
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files

abstract class ForgeGradleTestSpecification extends Specification {

    private static boolean DEBUG = true
    private static boolean LOG_LEVEL_INFO = false;
    private static boolean STACKTRACE = true

    public TestInfo state

    @BeforeEach
    void configureState(TestInfo state) {
        this.state = state
    }

    @TempDir
    protected File testProjectDir
    protected File propertiesFile
    protected File settingsFile
    protected File buildFile
    protected File localBuildCacheDirectory

    def setup() {
        localBuildCacheDirectory = new File(testProjectDir, 'local-cache')
        propertiesFile = new File(testProjectDir, 'gradle.properties')
        settingsFile = new File(testProjectDir, 'settings.gradle')
        buildFile = new File(testProjectDir, 'build.gradle')

        settingsFile << """
            buildCache {
                local {
                    directory '${localBuildCacheDirectory.toURI()}'
                }
            }
        """

        propertiesFile << """
                        org.gradle.console=rich
                        org.gradle.native=false
                        org.gradle.java.installations.auto-detect=true
                        """
    }

    protected GradleRunner gradleRunner() {
        def runner = GradleRunner.create()
                .withDebug(DEBUG)
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                .forwardOutput()

        return runner
    }

    BuildResult runTask(final String... tasks) {
        def runner = gradleRunner()

        def arguments = Lists.newArrayList(tasks)
        if (LOG_LEVEL_INFO) {
            arguments.add('--info')
        }
        if (STACKTRACE) {
            arguments.add('--stacktrace')
        }

        return runner.withArguments(arguments).build()
    }
}
