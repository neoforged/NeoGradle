package net.neoforged.gradle.platform;

import net.neoforged.gradle.dsl.userdev.extension.UserDev;
import net.neoforged.gradle.neoform.NeoFormPlugin;
import net.neoforged.gradle.platform.extensions.DynamicProjectExtension;
import net.neoforged.gradle.userdev.extension.UserDevExtension;
import net.neoforged.gradle.userdev.runtime.extension.UserDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class PlatformProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(@NotNull Project target) {
        target.getExtensions().create("dynamicProject", DynamicProjectExtension.class, target);
        
        
    }
}
