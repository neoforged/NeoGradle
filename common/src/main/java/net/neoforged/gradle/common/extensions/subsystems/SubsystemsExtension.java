package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.ConfigurableDSLElement;
import net.neoforged.gradle.dsl.common.extensions.subsystems.DecompilerLogLevel;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public abstract class SubsystemsExtension implements ConfigurableDSLElement<Subsystems>, Subsystems {
    private static final String PROPERTY_PREFIX = "neogradle.subsystems.";
    private static final String DEFAULT_RECOMPILER_MAX_MEMORY = "1g";
    private final Project project;

    @Inject
    public SubsystemsExtension(Project project) {
        this.project = project;

        // Decompiler default settings
        getDecompiler().getMaxMemory().convention(getStringProperty("decompiler.maxMemory"));
        getDecompiler().getMaxThreads().convention(getStringProperty("decompiler.maxThreads").map(Integer::parseUnsignedInt));
        getDecompiler().getLogLevel().convention(getStringProperty("decompiler.logLevel").map(s -> {
            try {
                return DecompilerLogLevel.valueOf(s.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new GradleException("Unknown DecompilerLogLevel: " + s + ". Available options: " + Arrays.toString(DecompilerLogLevel.values()));
            }
        }));
        getDecompiler().getJvmArgs().convention(getSpaceSeparatedListProperty("decompiler.jvmArgs").orElse(Collections.emptyList()));

        // Recompiler default settings
        getRecompiler().getArgs().convention(getSpaceSeparatedListProperty("recompiler.args").orElse(Collections.emptyList()));
        getRecompiler().getJvmArgs().convention(getSpaceSeparatedListProperty("recompiler.jvmArgs").orElse(Collections.emptyList()));
        getRecompiler().getMaxMemory().convention(getStringProperty("recompiler.maxMemory").orElse(DEFAULT_RECOMPILER_MAX_MEMORY));
    }

    private Provider<String> getStringProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(PROPERTY_PREFIX + propertyName);
    }

    private Provider<List<String>> getSpaceSeparatedListProperty(String propertyName) {
        return this.project.getProviders().gradleProperty(PROPERTY_PREFIX + propertyName)
                .map(s -> Arrays.asList(s.split("\\s+")));
    }

    @Override
    public Project getProject() {
        return project;
    }
}
