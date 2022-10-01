package net.minecraftforge.gradle.base

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

abstract class ForgeGradleTestSpecification extends Specification {

    private static boolean DEBUG = true;
    private static boolean STACKTRACE = true;


    @TempDir protected File testProjectDir
    protected File propertiesFile
    protected File settingsFile
    protected File buildFile

    def setup() {
        propertiesFile = new File(testProjectDir, 'gradle.properties')
        settingsFile = new File(testProjectDir, 'settings.gradle')
        buildFile = new File(testProjectDir, 'build.gradle')

        propertiesFile << """
                        org.gradle.java.installations.auto-detect=false
                        """
    }

    protected GradleRunner gradleRunner() {
        def runner = GradleRunner.create()
                .withDebug(DEBUG)
                .withPluginClasspath()
                .withProjectDir(testProjectDir)

        if (STACKTRACE) {
            runner.withArguments('--stacktrace')
        }

        return runner;
    }
}
