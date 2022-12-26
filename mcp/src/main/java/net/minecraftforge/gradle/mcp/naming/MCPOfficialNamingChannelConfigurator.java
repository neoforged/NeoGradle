package net.minecraftforge.gradle.mcp.naming;

import com.google.common.collect.ImmutableSet;
import net.minecraftforge.gradle.common.tasks.DownloadMavenArtifact;
import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.mcp.naming.tasks.ApplyMcpMappingsToSourceJar;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static net.minecraftforge.gradle.common.util.NamingConstants.Version.VERSION;

public final class MCPOfficialNamingChannelConfigurator {
    private static final MCPOfficialNamingChannelConfigurator INSTANCE = new MCPOfficialNamingChannelConfigurator();

    private MCPOfficialNamingChannelConfigurator() {
    }

    public static MCPOfficialNamingChannelConfigurator getInstance() {
        return INSTANCE;
    }

    public void configure(final Project project) {
        final Minecraft minecraftExtension = project.getExtensions().getByType(Minecraft.class);

        final NamingChannel namingChannel = minecraftExtension.getNamingChannelProviders().getByName("official");
        minecraftExtension.getNamingChannelProviders().remove(namingChannel);

        minecraftExtension.getNamingChannelProviders().register("official", namingChannelProvider -> {

        });
    }

}
