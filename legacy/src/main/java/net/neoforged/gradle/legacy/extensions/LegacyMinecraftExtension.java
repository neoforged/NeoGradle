package net.neoforged.gradle.legacy.extensions;

import net.neoforged.gradle.common.extensions.IExtensionCreator;
import net.neoforged.gradle.common.extensions.MinecraftExtension;
import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.util.NamingConstants;
import org.gradle.api.NamedDomainObjectContainer;
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

        getMappings().getChannel().set(getNamingChannels().getByName(channelName));
        getMappings().getVersion().put(NamingConstants.Version.VERSION, version);

        getProject().getLogger().warn(String.format("Using legacy mappings for channel: %s and version: %s.", channelName, version));
    }

    @Deprecated
    @Optional
    public abstract RegularFileProperty getAccessTransformer();

    @SuppressWarnings("unchecked")
    public NamedDomainObjectContainer<Run> getRuns() {
        return (NamedDomainObjectContainer<Run>) getProject().getExtensions().getByName(RunsConstants.Extensions.RUNS);
    }

    public static final class Creator implements IExtensionCreator<Minecraft> {

        @Override
        public Minecraft apply(Project project) {
            return project.getObjects().newInstance(LegacyMinecraftExtension.class, project);
        }
    }
}
