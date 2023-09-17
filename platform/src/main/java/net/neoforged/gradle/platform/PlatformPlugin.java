package net.neoforged.gradle.platform;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.jetbrains.annotations.NotNull;

public class PlatformPlugin implements Plugin<Object> {
   
   @Override
   public void apply(@NotNull Object target) {
      if (target instanceof Project) {
         ((Project) target).getPlugins().apply(PlatformProjectPlugin.class);
      } else if (target instanceof Settings) {
         ((Settings) target).getPlugins().apply(PlatformSettingsPlugin.class);
      }
   }
}
