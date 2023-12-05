package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.DecompilerLogLevel;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Locale;

public abstract class SubsystemsExtension implements ConfigurableDSLElement<Subsystems>, Subsystems {
    private static final String PROPERTY_PREFIX = "neogradle.subsystems.";
    private final Project project;

    @Inject
    public SubsystemsExtension(Project project) {
        this.project = project;

        ProviderFactory providers = project.getProviders();
        getDecompiler().getMaxMemory().convention(providers.gradleProperty(PROPERTY_PREFIX + "decompiler.maxMemory"));
        getDecompiler().getMaxThreads().convention(providers.gradleProperty(PROPERTY_PREFIX + "decompiler.maxThreads").map(Integer::parseUnsignedInt));
        getDecompiler().getLogLevel().convention(providers.gradleProperty(PROPERTY_PREFIX + "decompiler.logLevel").map(s -> {
            try {
                return DecompilerLogLevel.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new GradleException("Unknown DecompilerLogLevel: " + s + ". Available options: " + Arrays.toString(DecompilerLogLevel.values()));
            }
        }));
        getDecompiler().getJvmArgs().convention(providers.gradleProperty(PROPERTY_PREFIX + "decompiler.jvmArgs").map(s -> Arrays.asList(s.split("\\s+"))));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
