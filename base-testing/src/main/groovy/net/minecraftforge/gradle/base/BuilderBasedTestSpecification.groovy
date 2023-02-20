package net.minecraftforge.gradle.base


import com.google.common.collect.Maps
import net.minecraftforge.gradle.base.builder.Runtime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import spock.lang.Specification
import spock.lang.TempDir

import java.util.function.Consumer

abstract class BuilderBasedTestSpecification extends Specification {

    @TempDir
    protected File testTempDirectory
    protected File projectDirectory

    private boolean registeredRuntimesAreConfigured = false
    private Map<Runtime, Runtime> roots = Maps.newHashMap()
    private Map<String, Runtime> runtimes = Maps.newHashMap()

    @BeforeEach
    void configureState(TestInfo state) {
        this.projectDirectory = new File(testTempDirectory, state.getDisplayName())
        this.registeredRuntimesAreConfigured = true
        runtimes.values().forEach {runtime -> {
            final Runtime root = this.roots.get(runtime)
            final File workingDirectory = new File(projectDirectory, root.getProjectName())
            runtime.setup(root, workingDirectory)
        }}
    }

    protected Runtime create(final String name, final Consumer<Runtime.Builder> builderConsumer) {
        final Runtime.Builder builder = new Runtime.Builder(name)
        builderConsumer.accept(builder)
        final Runtime runtime = builder.create()

        if (this.registeredRuntimesAreConfigured) {
            final File workingDirectory = new File(projectDirectory, name)
            runtime.setup(runtime, workingDirectory)
        }

        this.runtimes.put(name, runtime)
        this.roots.put(runtime, runtime)
        return runtime
    }

    protected Runtime create(final String root, String name, final Consumer<Runtime.Builder> builderConsumer) {
        if (!this.runtimes.containsKey(root))
            throw new IllegalStateException("No runtime with name: $root")

        final Runtime rootRuntime = this.runtimes.get(root)

        return create(rootRuntime, name, builderConsumer)
    }

    protected Runtime create(final Runtime rootRuntime, String name, final Consumer<Runtime.Builder> builderConsumer) {
        final Runtime.Builder builder = new Runtime.Builder(name)
        builderConsumer.accept(builder)
        final Runtime runtime = builder.create()

        if (this.registeredRuntimesAreConfigured) {
            final File workingDirectory = new File(projectDirectory, name)
            runtime.setup(rootRuntime, workingDirectory)
        }

        this.runtimes.put(name, runtime)
        this.roots.put(runtime, rootRuntime)
        return runtime
    }
}
