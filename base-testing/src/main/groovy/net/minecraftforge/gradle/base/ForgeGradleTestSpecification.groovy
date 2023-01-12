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
import java.nio.file.Path
import java.nio.file.Paths

abstract class ForgeGradleTestSpecification extends Specification {

    private static final boolean DEBUG = true;
    private static final boolean DEBUG_PROJECT_DIR = false;

    public TestInfo state

    @BeforeEach
    void configureState(TestInfo state) {
        this.state = state
        this.getSpecificationContext()
    }

    @TempDir
    protected File internalTestDir;
    protected File testDir;

    protected File testProjectDir
    protected File propertiesFile
    protected File settingsFile
    protected File buildFile
    protected File localBuildCacheDirectory

    def setup() {
        if (DEBUG_PROJECT_DIR) {
            testDir = Paths.get("build", "test", this.getSpecificationContext().getCurrentSpec().getName(), this.getSpecificationContext().getCurrentFeature().getName()).toFile()
            testDir.mkdirs();
        }

        testProjectDir = !DEBUG_PROJECT_DIR ? internalTestDir : testDir;
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
                .withDebug(DEBUG)
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
