package net.neoforged.gradle.common.runtime.naming.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.runtime.tasks.DefaultExecute;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.util.Constants;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

@CacheableTask
public abstract class UnapplyOfficialMappingsToCompiledJar extends DefaultExecute {

    public UnapplyOfficialMappingsToCompiledJar() {
        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), Constants.SPECIALSOURCE));
        getProgramArguments().set(Lists.newArrayList("--in-jar", "{input}", "--out-jar", "{output}", "--srg-in", "{mappings}", "--live", "-r"));
        getMappings().fileProvider(getMinecraftVersion().map(minecraftVersion -> getProject().getExtensions().getByType(MinecraftArtifactCache.class).cacheVersionMappings(minecraftVersion, DistributionType.CLIENT)));

        getArguments().putRegularFile("input", getInput());
        getArguments().putRegularFile("mappings", getMappings());
    }

    @Input
    public abstract Property<String> getMinecraftVersion();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getMappings();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();
}
