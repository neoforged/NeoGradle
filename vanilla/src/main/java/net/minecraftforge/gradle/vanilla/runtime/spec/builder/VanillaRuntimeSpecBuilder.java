package net.minecraftforge.gradle.vanilla.runtime.spec.builder;

import net.minecraftforge.gradle.dsl.common.runtime.spec.builder.CommonRuntimeSpecBuilder;
import net.minecraftforge.gradle.vanilla.runtime.extensions.VanillaRuntimeExtension;
import net.minecraftforge.gradle.vanilla.runtime.spec.VanillaRuntimeSpec;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

@SuppressWarnings("unused")
public final class VanillaRuntimeSpecBuilder extends CommonRuntimeSpecBuilder<VanillaRuntimeSpec, VanillaRuntimeSpecBuilder> {

    private Provider<String> minecraftVersion;

    private Provider<String> fartVersion;
    private boolean hasConfiguredFartVersion = false;

    private Provider<String> forgeFlowerVersion;
    private boolean hasConfiguredForgeFlowerVersion = false;

    private Provider<String> accessTransformerApplierVersion;
    private boolean hasConfiguredAccessTransformerApplierVersion = false;

    private VanillaRuntimeSpecBuilder(Project project) {
        super(project);
    }

    public static VanillaRuntimeSpecBuilder create(Project project) {
        return new VanillaRuntimeSpecBuilder(project);
    }

    @Override
    protected VanillaRuntimeSpecBuilder getThis() {
        return this;
    }

    @Override
    protected void configureBuilder() {
        super.configureBuilder();
        final VanillaRuntimeExtension runtimeExtension = this.configureProject.getExtensions().getByType(VanillaRuntimeExtension.class);

        if (!this.hasConfiguredFartVersion) {
            this.fartVersion = runtimeExtension.getFartVersion();
        }

        if (!this.hasConfiguredForgeFlowerVersion) {
            this.forgeFlowerVersion = runtimeExtension.getForgeFlowerVersion();
        }

        if (!this.hasConfiguredAccessTransformerApplierVersion) {
            this.accessTransformerApplierVersion = runtimeExtension.getAccessTransformerApplierVersion();
        }
    }

    public VanillaRuntimeSpecBuilder withMinecraftVersion(final Provider<String> minecraftVersion) {
        this.minecraftVersion = minecraftVersion;
        return getThis();
    }

    public VanillaRuntimeSpecBuilder withMinecraftVersion(final String minecraftVersion) {
        if (minecraftVersion == null) // Additional null check for convenient loading of sides from dependencies.
            return getThis();

        return withMinecraftVersion(project.provider(() -> minecraftVersion));
    }

    public VanillaRuntimeSpecBuilder withFartVersion(final Provider<String> fartVersion) {
        this.fartVersion = fartVersion;
        this.hasConfiguredFartVersion = true;
        return getThis();
    }

    public VanillaRuntimeSpecBuilder withFartVersion(final String fartVersion) {
        if (fartVersion == null) // Additional null check for convenient loading of sides from dependencies.
            return getThis();

        return withFartVersion(project.provider(() -> fartVersion));
    }

    public VanillaRuntimeSpecBuilder withForgeFlowerVersion(final Provider<String> forgeFlowerVersion) {
        this.forgeFlowerVersion = forgeFlowerVersion;
        this.hasConfiguredForgeFlowerVersion = true;
        return getThis();
    }

    public VanillaRuntimeSpecBuilder withForgeFlowerVersion(final String forgeFlowerVersion) {
        if (forgeFlowerVersion == null) // Additional null check for convenient loading of sides from dependencies.
            return getThis();

        return withForgeFlowerVersion(project.provider(() -> forgeFlowerVersion));
    }

    public VanillaRuntimeSpecBuilder withAccessTransformerApplierVersion(final Provider<String> accessTransformerApplierVersion) {
        this.accessTransformerApplierVersion = accessTransformerApplierVersion;
        this.hasConfiguredAccessTransformerApplierVersion = true;
        return getThis();
    }

    public VanillaRuntimeSpecBuilder withAccessTransformerApplierVersion(final String accessTransformerApplierVersion) {
        if (accessTransformerApplierVersion == null) // Additional null check for convenient loading of sides from dependencies.
            return getThis();

        return withAccessTransformerApplierVersion(project.provider(() -> accessTransformerApplierVersion));
    }

    @Override
    public VanillaRuntimeSpec build() {
        return new VanillaRuntimeSpec(project, configureProject, namePrefix, side.get(), preTaskAdapters, postTaskAdapters, minecraftVersion.get(), fartVersion.get(), forgeFlowerVersion.get(), accessTransformerApplierVersion.get());
    }
}
