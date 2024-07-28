package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runtime.naming.NamingChannel;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class DisplayMappingsLicenseTask extends NeoGradleBase {

    public DisplayMappingsLicenseTask() {
        getProject().getTasks().configureEach(task -> {
            if (task instanceof DisplayMappingsLicenseTask)
                return;

            try {
                task.dependsOn(this);
            } catch (IllegalStateException exception) {
                //This triggers for example when you run :tasks, which is a dynamic task added during runtime.
            }
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
    }

    private void displayWarning(NamingChannel channel) {
        if (!getLicense().isPresent())
            return;
        
        final String license = getLicense().get();

        final String warning = buildWarning();

        getLogger().warn(warning);
        getLogger().warn(Arrays.stream(license.split("\n")).map(line -> String.format("WARNING: %s", line)).collect(Collectors.joining("\n")));
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
    public abstract Property<String> getLicense();
}
