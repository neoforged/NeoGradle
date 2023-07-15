package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.runtime.naming.NamingChannelProvider;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.tasks.ForgeGradleBase;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class DisplayMappingsLicenseTask extends ForgeGradleBase {

    public DisplayMappingsLicenseTask() {
        getProject().getTasks().configureEach(task -> {
            if (task instanceof DisplayMappingsLicenseTask)
                return;

            task.dependsOn(this);
        });

        this.setOnlyIf(task -> {
            final Mappings mappingsExtension = getProject().getExtensions().getByType(Mappings.class);
            return mappingsExtension.getChannel().isPresent() && !mappingsExtension.getChannel().get().getHasAcceptedLicense().getOrElse(false);
        });
    }

    @TaskAction
    public void run() {
        final Mappings mappingsExtension = getProject().getExtensions().getByType(Minecraft.class).getMappings();

        displayWarning(mappingsExtension.getChannel().get());

        if (!getUpdateChannel().isPresent() || getUpdateChannel().get().equals(mappingsExtension.getChannel().get())) return;

        displayWarning(getUpdateChannel().get());
    }

    private void displayWarning(NamingChannel channel) {
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
