package net.neoforged.gradle.dsl.common.runtime.spec;

import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

/**
 * Defines a specification of a runtime in a project.
 *
 * @param project The project this specification is for.
 * @param name The lazy provider for the name of the runtime.
 * @param version The lazy provider for the version of the runtime.
 * @param minecraftVersion The lazy provider for the Minecraft version of the runtime.
 * @param distribution The lazy provider for the distribution type of the runtime.
 */
public record Specification(
        Project project,
        String name,
        Provider<String> version,
        Provider<String> minecraftVersion,
        Provider<DistributionType> distribution
) {

    public Provider<String> identifier() {
        // Capitalize the first letter of the distribution type name
        final Provider<String> distributionTypeName = distribution()
                .map(DistributionType::getName)
                .map(String::toLowerCase)
                .map(StringUtils::capitalize);

        // Combine the name, distribution type name, and version
        return distributionTypeName.zip(version(), (type, version) -> "%s%s%s".formatted(name(), type, version));
    }
}
