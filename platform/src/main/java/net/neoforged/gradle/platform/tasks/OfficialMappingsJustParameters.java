package net.neoforged.gradle.platform.tasks;

import net.minecraftforge.srgutils.IMappingBuilder;
import net.minecraftforge.srgutils.IMappingFile;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.IOException;

@CacheableTask
public abstract class OfficialMappingsJustParameters extends DefaultRuntime implements WithOutput {
    @Inject
    public OfficialMappingsJustParameters() {
        getOutputFileName().set("output.tsrg");
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @TaskAction
    void exec() throws IOException {
        var source = IMappingFile.load(getInput().getAsFile().get());
        var builder = IMappingBuilder.create();
        source.getClasses().forEach(cls -> {
            var c = builder.addClass(cls.getMapped(), cls.getMapped());
            cls.getMethods().forEach(mtd -> {
                if (mtd.getParameters().isEmpty()) return;

                var m = c.method(mtd.getMappedDescriptor(), mtd.getMapped(), mtd.getMapped());
                mtd.getParameters().forEach(par -> m.parameter(par.getIndex(), par.getOriginal(), par.getMapped()));
            });
        });
        builder.build().write(getOutput().get().getAsFile().toPath(), IMappingFile.Format.TSRG2);
    }
}
