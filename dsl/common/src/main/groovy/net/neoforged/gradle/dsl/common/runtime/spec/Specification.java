package net.neoforged.gradle.dsl.common.runtime.spec;

import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.util.NamedRecord;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Defines a specification of a runtime in a project.
 *
 * @param project The project this specification is for.
 * @param identifier The identifier of the runtime.
 * @param version The version of the runtime.
 * @param minecraftVersion The minecraft version of the runtime.
 * @param distribution The distribution type of the runtime.
 */
public record Specification(
        Project project,
        String identifier,
        String version,
        String minecraftVersion,
        DistributionType distribution
) implements NamedRecord {

    public @NotNull String name() {
        // Capitalize the first letter of the distribution type identifier
        final String type = StringUtils.capitalize(distribution()
                .getName()
                .toLowerCase(Locale.ROOT));

        // Combine the identifier, distribution type identifier, and version
        return "%s%s%s".formatted(identifier(), type, version());
    }
}
