package net.minecraftforge.gradle.legacy;

import groovy.lang.Closure;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.userdev.UserDevPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.Map;

@SuppressWarnings("unchecked")
public class LegacyProjectPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.getPlugins().apply(UserDevPlugin.class);

        final Minecraft minecraft = project.getExtensions().getByType(Minecraft.class);

        minecraft.

        minecraft.getExtensions().add("mappings", new Closure<Void>(minecraft, minecraft) {
            @Override
            public Void call(Object... args) {
                if (args.length != 1) {
                    throw new IllegalArgumentException("Expected 1 argument, got " + args.length);
                }

                final Map<String, Object> parameters = (Map<String, Object>) args[0];
                if (!parameters.containsKey("channel")) {
                    throw new IllegalArgumentException("Missing required parameter 'channel'");
                }

                final String channel = (String) parameters.get("channel");
                final String version = (String) parameters.get("version");

                if (channel == null) {
                    throw new IllegalArgumentException("Missing required parameter 'channel'");
                }

                if (channel != "official") {
                    throw new IllegalArgumentException("Only official channel is supported");
                }

                minecraft.getMappings().getChannel().set(minecraft.getNamingChannelProviders().getByName(channel));
                minecraft.getMappings().getVersion().put("minecraft", version);

                try {
                    return Void.TYPE.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
