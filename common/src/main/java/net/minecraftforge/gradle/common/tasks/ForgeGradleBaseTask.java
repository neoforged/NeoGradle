package net.minecraftforge.gradle.common.tasks;

import net.minecraftforge.gradle.common.util.TransformerUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;

import java.io.File;

public abstract class ForgeGradleBaseTask extends DefaultTask {

    protected Provider<File> ensureFileWorkspaceReady(final RegularFileProperty fileProvider) {
        return ensureFileWorkspaceReady(fileProvider.getAsFile());
    }

    protected Provider<File> ensureFileWorkspaceReady(final Provider<File> fileProvider) {
        return fileProvider.map(TransformerUtils.guard(
                f -> {
                    if (f.exists()) {
                        f.delete();
                        return f;
                    }

                    f.getParentFile().mkdirs();
                    return f;
                }
        ));
    }
}
