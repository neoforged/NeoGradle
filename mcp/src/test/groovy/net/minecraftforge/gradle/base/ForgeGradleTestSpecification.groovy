package net.minecraftforge.gradle.base

import com.google.common.collect.Lists
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import spock.lang.Specification
import spock.lang.TempDir

abstract class ForgeGradleTestSpecification extends Specification {

    private static boolean DEBUG = false
    private static boolean LOG_LEVEL_INFO = false;
    private static boolean STACKTRACE = true
    private static boolean EMIT_LOGS = false

    public TestInfo state

    @BeforeEach
    void setup(TestInfo state) {
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
                        org.gradle.java.installations.auto-detect=false
                        org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
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

        def buildResult = runner.withArguments(arguments).build()

        if (EMIT_LOGS) {
            final File testLogFile = new File('testlogs', state.getDisplayName() + ".log")
            testLogFile.getParentFile().mkdirs()
            testLogFile.text = buildResult.output
        }

        return buildResult
    }
}
