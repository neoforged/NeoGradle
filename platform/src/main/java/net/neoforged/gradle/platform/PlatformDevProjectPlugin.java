package net.neoforged.gradle.platform;

import net.neoforged.gradle.neoform.NeoFormPlugin;
import net.neoforged.gradle.platform.runtime.runtime.extension.RuntimeDevRuntimeExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class PlatformDevProjectPlugin implements Plugin<Project> {
   
   @Override
   public void apply(Project target) {
      target.getPlugins().apply(NeoFormPlugin.class);
      target.getExtensions().create("platformDevRuntime", RuntimeDevRuntimeExtension.class, target);
   }
}
