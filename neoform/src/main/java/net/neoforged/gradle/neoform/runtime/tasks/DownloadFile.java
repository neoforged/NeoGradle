package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.util.FileDownloadingUtils;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class DownloadFile extends DefaultRuntime {

    public DownloadFile() {
        getDownloadInfo().finalizeValueOnRead();
        getIsOffline().convention(getProject().getGradle().getStartParameter().isOffline());
    }

    @TaskAction
    public void run() throws Exception {
        if (getDownloadInfo().isPresent()) {
            final FileDownloadingUtils.DownloadInfo info = getDownloadInfo().get();
            doDownloadFrom(info);
        } else {
            throw new IllegalStateException("No download info provided");
        }
    }

    protected void doDownloadFrom(FileDownloadingUtils.DownloadInfo info) throws IOException {
        final File outputFile = ensureFileWorkspaceReady(getOutput());

        FileDownloadingUtils.downloadTo(getIsOffline().get(), info, outputFile);

        setDidWork(true);
    }

    @Nested
    @Optional
    public abstract Property<FileDownloadingUtils.DownloadInfo> getDownloadInfo();
    
    @Input
    @Optional
    public abstract Property<Boolean> getIsOffline();

}
