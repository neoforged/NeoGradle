package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class RenderDocDownloaderTask extends NeoGradleBase {

    public RenderDocDownloaderTask() {
        getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
        getRenderDocVersion().convention("1.33"); // Current default.
        getRenderDocOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc/download"));
        getRenderDocInstallationDirectory().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc/installation"));
        getRenderDocLibraryFile().fileProvider(
                getRenderDocInstallationDirectory().map(dir -> getOSSpecificRenderDocLibraryFile(dir.getAsFile()))
        );

        getOutputs().upToDateWhen(task -> false);
    }

    @TaskAction
    public void doDownload() throws IOException {
        final File outputRoot = getRenderDocInstallationDirectory().get().getAsFile();
        if (outputRoot.exists() && outputRoot.isDirectory()) {
            final File renderDocLibraryFile = getOSSpecificRenderDocLibraryFile(outputRoot);
            if (renderDocLibraryFile.exists() && renderDocLibraryFile.isFile()) {
                //setDidWork(false);
                //return;
            }
        }

        final String url = getOSSpecificRenderDocUrl();

        final File output = getRenderDocInstallationDirectory().get().getAsFile();
        if (output.exists()) {
            if (output.isFile()) {
                output.delete();
                output.mkdirs();
            } else {
                FileUtils.cleanDirectory(output);
            }
        } else {
            output.mkdirs();
        }

        final FileDownloadingUtils.DownloadInfo downloadInfo = new FileDownloadingUtils.DownloadInfo(url, null, null, null, null);
        final File compressedDownloadTarget = new File(getRenderDocOutputDirectory().get().getAsFile(), getOSSpecificFileName());
        FileDownloadingUtils.downloadTo(getIsOffline().getOrElse(false), downloadInfo, compressedDownloadTarget);

        extractOSSpecific(compressedDownloadTarget);
    }

    @Input
    @Optional
    public abstract Property<Boolean> getIsOffline();

    @Input
    public abstract Property<String> getRenderDocVersion();

    @OutputDirectory
    public abstract DirectoryProperty getRenderDocOutputDirectory();

    @Internal
    public abstract DirectoryProperty getRenderDocInstallationDirectory();

    @Internal
    public abstract RegularFileProperty getRenderDocLibraryFile();

    private File getOSSpecificRenderDocLibraryFile(final File root) {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            return new File(root, "RenderDoc_%s_64/renderdoc.dll".formatted(getRenderDocVersion().get()));
        }

        if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            return new File(root, "renderdoc_%s/lib/librenderdoc.so".formatted(getRenderDocVersion().get()));
        }

        throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
    }

    private File getOSSpecificRenderDocExecutableFile(final File root) {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            throw new IllegalStateException("Not implemented yet");
        }

        if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            return new File(root, "renderdoc_%s/bin/qrenderdoc".formatted(getRenderDocVersion().get()));
        }

        throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
    }

    private String getOSSpecificRenderDocUrl() {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            return "https://renderdoc.org/stable/1.33/RenderDoc_%s_64.zip".formatted(getRenderDocVersion().get());
        }

        if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            return "https://renderdoc.org/stable/1.33/renderdoc_%s.tar.gz".formatted(getRenderDocVersion().get());
        }

        throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
    }

    private String getOSSpecificFileName() {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            return "renderdoc.zip";
        }

        if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            return "renderdoc.tar.gz";
        }

        throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
    }

    private void extractOSSpecific(final File input) {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            extractWindows(input);
        } else if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            extractLinux(input);
        } else {
            throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
        }
    }

    private void extractWindows(final File input) {
        final File output = getRenderDocInstallationDirectory().get().getAsFile();

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output.toPath());
        getArchiveOperations().zipTree(input).visit(visitor);
    }

    private void extractLinux(final File input) {
        final File output = getRenderDocInstallationDirectory().get().getAsFile();

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output.toPath());
        getArchiveOperations().tarTree(input).visit(visitor);

        final File executable = getOSSpecificRenderDocExecutableFile(output);
        executable.setExecutable(true);
    }


}
