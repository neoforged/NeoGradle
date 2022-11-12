package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.extensions.MappingsExtension;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.common.runtime.naming.NamingChannelProvider;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class DisplayMappingsLicenseTask extends ForgeGradleBaseTask {

    public DisplayMappingsLicenseTask() {
        getProject().getTasks().configureEach(task -> {
            if (task instanceof DisplayMappingsLicenseTask)
                return;

            task.dependsOn(this);
        });

        this.setOnlyIf(task -> {
            final MappingsExtension mappingsExtension = getProject().getExtensions().getByType(MinecraftExtension.class).getMappings();
            return mappingsExtension.getMappingChannel().isPresent() && !mappingsExtension.getMappingChannel().get().getHasAcceptedLicense().getOrElse(false);
        });
    }

    @TaskAction
    public void run() {
        final MappingsExtension mappingsExtension = getProject().getExtensions().getByType(MinecraftExtension.class).getMappings();

        displayWarning(mappingsExtension.getMappingChannel().get());

        if (!getUpdateChannel().isPresent() || getUpdateChannel().get().equals(mappingsExtension.getMappingChannel().get())) return;

        displayWarning(getUpdateChannel().get());
    }

    private void displayWarning(NamingChannelProvider channel) {
        Provider<String> license = channel.getLicenseText();

        String warning = buildWarning();

        getLogger().warn(warning);
        if (license.isPresent()) {
            getLogger().warn(Arrays.stream(license.get().split("\n")).map(line -> String.format("WARNING: %s", line)).collect(Collectors.joining("\n")));
        }
    }

    private String buildWarning() {
        return "WARNING: This project is configured to use obfuscation mappings with a custom license attached.\n" +
               "WARNING: These mapping fall under their associated license, you should be fully aware of this license.\n" +
               "WARNING: For the latest license text, refer below.\n" +
               "WARNING: You can hide this warning by accepting its license.\n" +
               "WARNING: See the naming channels documentation for more information on how to do that.";
    }

    @Input
    @Optional
    public abstract Property<NamingChannelProvider> getUpdateChannel();

    @Input
    @Optional
    public abstract MapProperty<String, String> getUpdateVersion();
}
