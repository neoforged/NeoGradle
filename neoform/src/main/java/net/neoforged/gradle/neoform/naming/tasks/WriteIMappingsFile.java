package net.neoforged.gradle.neoform.naming.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;

@CacheableTask
public abstract class WriteIMappingsFile extends DefaultRuntime implements WithOutput {

    public WriteIMappingsFile() {
        super();
        getFormat().convention(IMappingFile.Format.TSRG2);
        getReversed().convention(false);
        Provider<String> outputExtension = getFormat().map(format -> {
            switch (format) {
                case SRG:
                    return "srg";
                case TSRG2:
                    return "tsrg";
                default:
                    return "txt";
            }
        });
        getArguments().put("outputExtension", outputExtension);
    }

    @TaskAction
    public void write() throws IOException {
        getMappings().get().write(
                getOutput().getAsFile().get().toPath(),
                getFormat().get(),
                getReversed().get()
        );
    }

    @Input
    public abstract Property<CacheableIMappingFile> getMappings();

    @Input
    public abstract Property<IMappingFile.Format> getFormat();

    @Input
    public abstract Property<Boolean> getReversed();
}
