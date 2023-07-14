package net.minecraftforge.gradle.legacy.extensions;

import net.minecraftforge.gradle.common.extensions.IExtensionCreator;
import net.minecraftforge.gradle.common.extensions.MinecraftExtension;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.util.NamingConstants;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Optional;

import javax.inject.Inject;
import java.util.Map;

@Deprecated
public abstract class LegacyMinecraftExtension extends MinecraftExtension {

    @Inject
    public LegacyMinecraftExtension(Project project) {
        super(project);

        final AccessTransformers accessTransformers = getAccessTransformers();
        accessTransformers.getFiles().from(getAccessTransformer());
    }

    @Deprecated
    public void mappings(final Map<String, String> mappingsPayload) {
        if (!mappingsPayload.containsKey("channel")) {
            throw new IllegalArgumentException("Missing required parameter 'channel'");
        }

        final String channel = mappingsPayload.get("channel");
        final String version = mappingsPayload.get("version");

        mappings(channel, version);
    }

    @Deprecated
    public void mappings(final String channelName, final String version) {
        if (channelName == null) {
            throw new IllegalArgumentException("Missing required parameter 'channel'");
        }

        if (!channelName.equals("official")) {
            throw new IllegalArgumentException("Only official channel is supported");
        }

        getMappings().getChannel().set(getNamingChannelProviders().getByName(channelName));
        getMappings().getVersion().put(NamingConstants.Version.VERSION, version);

        getProject().getLogger().warn(String.format("Using legacy mappings for channel: %s and version: %s.", channelName, version));
    }

    @Deprecated
    @Optional
    public abstract RegularFileProperty getAccessTransformer();

    public static final class Creator implements IExtensionCreator<Minecraft> {

        @Override
        public Minecraft apply(Project project) {
            return project.getObjects().newInstance(LegacyMinecraftExtension.class, project);
        }
    }
}
