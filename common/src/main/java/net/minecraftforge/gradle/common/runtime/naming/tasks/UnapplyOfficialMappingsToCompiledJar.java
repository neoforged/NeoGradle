package net.minecraftforge.gradle.common.runtime.naming.tasks;

import com.google.common.collect.Lists;
import net.minecraftforge.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.common.tasks.JavaToolExecutingTask;
import net.minecraftforge.gradle.dsl.common.util.DistributionType;
import net.minecraftforge.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.minecraftforge.gradle.common.util.Utils;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class UnapplyOfficialMappingsToCompiledJar extends JavaToolExecutingTask implements WithOutput {

    public UnapplyOfficialMappingsToCompiledJar() {
        getExecutingArtifact().set(Utils.SPECIALSOURCE);
        getProgramArguments().set(Lists.newArrayList("--in-jar", "{input}", "--out-jar", "{output}", "--srg-in", "{mappings}", "--live", "-r"));
        getMappings().fileProvider(getMinecraftVersion().map(minecraftVersion -> getProject().getExtensions().getByType(MinecraftArtifactCache.class).cacheVersionMappings(minecraftVersion.getFull(), DistributionType.CLIENT)));

        getArguments().put("input", getInput().getAsFile().map(File::getAbsolutePath));
        getArguments().put("mappings", getMappings().getAsFile().map(File::getAbsolutePath));
    }

    @Override
    public void execute() throws Throwable {
        super.execute();
    }

    @Input
    public abstract Property<CacheableMinecraftVersion> getMinecraftVersion();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getMappings();

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getInput();
}
