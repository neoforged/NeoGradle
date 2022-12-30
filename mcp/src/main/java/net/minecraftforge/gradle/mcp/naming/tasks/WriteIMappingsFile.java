package net.minecraftforge.gradle.mcp.naming.tasks;

import net.minecraftforge.gradle.common.runtime.tasks.DefaultRuntime;
import net.minecraftforge.gradle.dsl.common.tasks.WithOutput;
import net.minecraftforge.gradle.mcp.util.CacheableIMappingFile;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

@CacheableTask
public abstract class WriteIMappingsFile extends DefaultRuntime implements WithOutput {

    public WriteIMappingsFile() {
        super();
    }

    @TaskAction
    public void write() throws IOException {
        getMappings().get().write(
                getOutput().getAsFile().get().toPath(),
                IMappingFile.Format.TSRG2,
                false
        );
    }

    @Input
    public abstract Property<CacheableIMappingFile> getMappings();
}
