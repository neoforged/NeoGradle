package net.neoforged.gradle.common.runtime.naming.tasks;

import com.google.common.collect.Lists;
import net.neoforged.gradle.common.runtime.tasks.Execute;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.util.CacheableMinecraftVersion;
import net.neoforged.gradle.dsl.common.util.Constants;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.util.RenameConstants;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import java.io.File;

@CacheableTask
public abstract class ApplyOfficialMappingsToCompiledJar extends Execute implements WithOutput {

    public ApplyOfficialMappingsToCompiledJar() {
        super();

        getExecutingJar().set(ToolUtilities.resolveTool(getProject(), Constants.FART));
        getProgramArguments().set(Lists.newArrayList(RenameConstants.DEFAULT_PROGRAMM_ARGS));
        getJvmArguments().set(Lists.newArrayList(RenameConstants.DEFAULT_JVM_ARGS));
        getMappings().fileProvider(getMinecraftVersion().map(minecraftVersion -> getProject().getExtensions().getByType(MinecraftArtifactCache.class).cacheVersionMappings(minecraftVersion.getFull(), DistributionType.CLIENT)));

        getArguments().put("input", getInput().getAsFile().map(File::getAbsolutePath));
        getArguments().put("mappings", getMappings().getAsFile().map(File::getAbsolutePath));
        getArguments().put("libraries", getLibraries().getAsFile().map(File::getAbsolutePath));

        getOutput().convention(getOutputDirectory().map(d -> d.file("output.jar")));
    }

    @Override
    public void execute() throws Throwable {
        super.execute();
    }

    @Input
    public abstract Property<CacheableMinecraftVersion> getMinecraftVersion();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getMappings();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLibraries();
}
