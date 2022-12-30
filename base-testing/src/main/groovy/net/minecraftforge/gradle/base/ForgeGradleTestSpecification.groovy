package net.minecraftforge.gradle.base

import com.google.common.collect.Lists
import net.minecraftforge.gradle.base.util.LoggerWriter
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.annotations.NotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import org.slf4j.LoggerFactory
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files

abstract class ForgeGradleTestSpecification extends Specification {

    private static final boolean DEBUG = true;
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
                        org.gradle.jvmargs=-Xmx4g
                        """
    }

    protected GradleRunner gradleRunner() {
        def logger = LoggerFactory.getLogger("Test")

        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(testProjectDir)
                //.withDebug(DEBUG)
                .forwardStdOutput(new LoggerWriter(logger, LoggerWriter.Level.INFO))
                .forwardStdError(new LoggerWriter(logger, LoggerWriter.Level.ERROR))

        return runner
    }

    BuildResult runTask(final String... tasks) {
        def runner = gradleRunner()

        def arguments = Lists.newArrayList(tasks)
        arguments.add('--stacktrace')
        arguments.add('--info')
        return runner.withArguments(arguments).build()
    }
}
