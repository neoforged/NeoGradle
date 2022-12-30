package net.minecraftforge.gradle.vanilla.runtime.spec;

import com.google.common.collect.Multimap;
import net.minecraftforge.gradle.common.runtime.specification.CommonRuntimeSpec;
import net.minecraftforge.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import org.gradle.api.Project;

/**
 * Defines a specification for a MCP runtime.
 */
public final class VanillaRuntimeSpec extends CommonRuntimeSpec {
    private static final long serialVersionUID = -3537760562547500214L;

    private final String minecraftVersion;
    private final String fartVersion;
    private final String forgeFlowerVersion;
    private final String accessTransformerApplierVersion;

    public VanillaRuntimeSpec(Project project, Project configureProject, String name, DistributionType side, Multimap<String, TaskTreeAdapter> preTaskTypeAdapters, Multimap<String, TaskTreeAdapter> postTypeAdapters, String minecraftVersion, String fartVersion, String forgeFlowerVersion, String accessTransformerApplierVersion) {
        super(project, configureProject, name, side, preTaskTypeAdapters, postTypeAdapters);
        this.minecraftVersion = minecraftVersion;
        this.fartVersion = fartVersion;
        this.forgeFlowerVersion = forgeFlowerVersion;
        this.accessTransformerApplierVersion = accessTransformerApplierVersion;
    }

    public String getMinecraftVersion() {
        return minecraftVersion;
    }

    public String fartVersion() {
        return fartVersion;
    }

    public String forgeFlowerVersion() {
        return forgeFlowerVersion;
    }

    public String accessTransformerApplierVersion() {
        return accessTransformerApplierVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VanillaRuntimeSpec)) return false;
        if (!super.equals(o)) return false;

        VanillaRuntimeSpec that = (VanillaRuntimeSpec) o;

        if (!minecraftVersion.equals(that.minecraftVersion)) return false;
        if (!fartVersion.equals(that.fartVersion)) return false;
        if (!forgeFlowerVersion.equals(that.forgeFlowerVersion)) return false;
        return accessTransformerApplierVersion.equals(that.accessTransformerApplierVersion);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + minecraftVersion.hashCode();
        result = 31 * result + fartVersion.hashCode();
        result = 31 * result + forgeFlowerVersion.hashCode();
        result = 31 * result + accessTransformerApplierVersion.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "VanillaRuntimeSpec{" +
                "minecraftVersion='" + minecraftVersion + '\'' +
                ", fartVersion='" + fartVersion + '\'' +
                ", forgeFlowerVersion='" + forgeFlowerVersion + '\'' +
                ", accessTransformerApplierVersion='" + accessTransformerApplierVersion + '\'' +
                '}';
    }
}
