package net.minecraftforge.gradle.mcp.naming;

import net.minecraftforge.gradle.dsl.common.extensions.Minecraft;
import net.minecraftforge.gradle.dsl.common.runtime.naming.NamingChannel;
import net.minecraftforge.gradle.dsl.common.runtime.naming.TaskBuildingContext;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.Runtime;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

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

        minecraftExtension.getNamingChannelProviders().register("official", newOfficialProvider -> {
            newOfficialProvider.getMinecraftVersionExtractor().convention(namingChannel.getMinecraftVersionExtractor());
            newOfficialProvider.getApplySourceMappingsTaskBuilder().convention(context -> this.adaptApplySourceMappingsTask(context, namingChannel));
        });
    }

    private TaskProvider<? extends Runtime> adaptApplySourceMappingsTask(TaskBuildingContext context, NamingChannel namingChannel) {
        final TaskProvider<? extends Runtime> applySourceMappingsTask = namingChannel.getApplySourceMappingsTaskBuilder().get().build(context);

        context.get

        applySourceMappingsTask.configure(task -> {
        });
    }

}
