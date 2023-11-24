package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.util.Arrays;

public abstract class SubsystemsExtension implements ConfigurableDSLElement<Subsystems>, Subsystems {
    private final Project project;

    @Inject
    public SubsystemsExtension(Project project) {
        this.project = project;

        ProviderFactory providers = project.getProviders();
        getDecompiler().getMaxMemory().convention(providers.gradleProperty("neogradle.subsystems.decompiler.maxMemory"));
        getDecompiler().getMaxThreads().convention(providers.gradleProperty("neogradle.subsystems.decompiler.maxThreads").map(Integer::parseUnsignedInt));
        getDecompiler().getLogLevel().convention(providers.gradleProperty("neogradle.subsystems.decompiler.logLevel"));
        getDecompiler().getJvmArgs().convention(providers.gradleProperty("neogradle.subsystems.decompiler.jvmArgs").map(s -> Arrays.asList(s.split("\\s+"))));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
