package net.minecraftforge.gradle.mcp.extensions;

import groovy.cli.Option;
import groovy.lang.Closure;
import net.minecraftforge.gradle.mcp.util.MappingUtils;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.Map;

/**
 * Defines a holder for mapping artifact specification.
 */
public abstract class MappingsExtension implements Configurable<MappingsExtension> {

    private final Project project;

    @Inject
    public MappingsExtension(Project project) {
        this.project = project;

        getMappingArtifact().convention(
                getMappingChannel().flatMap(channel -> getMappingVersion().map(version -> MappingUtils.buildMappingArtifact(channel, version)))
        );
    }

    /**
     * @return The project this extension belongs to.
     */
    public Project getProject() {
        return project;
    }

    /**
     * The exact artifact to use as a source of the mappings.
     * If left unspecified a combination of the channel and version properties of this extension is used.
     *
     * @return The exact artifact to use as a source of mappings.
     */
    @Input
    public abstract Property<String> getMappingArtifact();

    /**
     * The channel to pull the mappings from.
     * If no direct artifact is specified then this channel is used as the artifacts' id.
     *
     * @return The channel to pull the mappings from.
     */
    @Input
    @Optional
    public abstract Property<String> getMappingChannel();

    /**
     * The version to pull the mappings from.
     * If no direct artifact is specified then this version is used as the artifacts' version.
     *
     * @return The version to pull the mappings from.
     */
    @Input
    @Optional
    public abstract Property<String> getMappingVersion();

    /**
     * Configures this object using the given closure.
     * @param closure The closure to configure with.
     * @return This object.
     */
    @SuppressWarnings("deprecation") //For now this is usable, if it ever gets removed, which is doubtfully, switch to gradle's own internal configure util.
    @Override
    public MappingsExtension configure(Closure closure) {
        return ConfigureUtil.configureSelf(closure, this);
    }

    /**
     * Configures the mappings using the data given from the map.
     * @param source The source.
     * @return This object.
     */
    @SuppressWarnings("deprecation") //For now this is usable, if it ever gets removed, which is doubtfully, switch to gradle's own internal configure util.
    public MappingsExtension configure(Map<?,?> source) {
        return ConfigureUtil.configureByMap(source, this);
    }
}
