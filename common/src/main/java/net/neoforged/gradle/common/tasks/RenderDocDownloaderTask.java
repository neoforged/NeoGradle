package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.util.FileDownloadingUtils;
import net.neoforged.gradle.common.util.VersionJson;
import net.neoforged.gradle.dsl.common.tasks.NeoGradleBase;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.apache.commons.io.FileUtils;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.internal.enterprise.test.FileProperty;

import java.io.File;
import java.io.IOException;

@CacheableTask
public abstract class RenderDocDownloaderTask extends NeoGradleBase {

    public RenderDocDownloaderTask() {
        getIsOffline().set(getProject().getGradle().getStartParameter().isOffline());
        getRenderDocVersion().convention("1.33"); // Current default.
        getRenderDocOutputDirectory().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc/download"));
        getRenderDocInstallationPath().convention(getProject().getLayout().getBuildDirectory().dir("renderdoc/installation"));
        getRenderDocLibraryFile().fileProvider(
                getRenderDocOutputDirectory().map(dir -> getOSSpecificRenderDocLibraryFile(dir.getAsFile()))
        );
    }

    @TaskAction
    public void doDownload() throws IOException {
        final File outputRoot = getRenderDocOutputDirectory().get().getAsFile();
        if (outputRoot.exists() && outputRoot.isDirectory()) {
            final File renderDocLibraryFile = getOSSpecificRenderDocLibraryFile(outputRoot);
            if (renderDocLibraryFile.exists() && renderDocLibraryFile.isFile()) {
                setDidWork(false);
                return;
            }
        }

        final String url = getOSSpecificRenderDocUrl();

        final File output = getRenderDocOutputDirectory().get().getAsFile();
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
        final File compressedDownloadTarget = new File(output, getOSSpecificFileName());
        FileDownloadingUtils.downloadTo(getIsOffline().getOrElse(false), downloadInfo, compressedDownloadTarget);

        extractOSSpecific(output);
    }

    @Input
    @Optional
    public abstract Property<Boolean> getIsOffline();

    @Input
    public abstract Property<String> getRenderDocVersion();

    @OutputDirectory
    public abstract DirectoryProperty getRenderDocOutputDirectory();

    @Internal
    public abstract DirectoryProperty getRenderDocInstallationPath();

    @OutputFile
    public abstract RegularFileProperty getRenderDocLibraryFile();

    private File getOSSpecificRenderDocLibraryFile(final File root) {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            return new File(root, "renderdoc.dll");
        }

        if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            return new File(root, "lib/librenderdoc.so");
        }

        throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
    }

    private String getOSSpecificRenderDocUrl() {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            return "https://renderdoc.org/stable/1.33/RenderDoc_" + getRenderDocVersion().get() + "_64.zip";
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

    private void extractOSSpecific(final File root) {
        if (VersionJson.OS.getCurrent() == VersionJson.OS.WINDOWS) {
            extractWindows(root);
        } else if (VersionJson.OS.getCurrent() == VersionJson.OS.LINUX) {
            extractLinux(root);
        } else {
            throw new IllegalStateException("Unsupported OS: " + VersionJson.OS.getCurrent().name());
        }
    }

    private void extractWindows(final File root) {
        final File zip = new File(root, "renderdoc.zip");
        final File output = getRenderDocInstallationPath().get().getAsFile();

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output.toPath());
        getArchiveOperations().zipTree(zip).visit(visitor);
    }

    private void extractLinux(final File root) {
        final File tar = new File(root, "renderdoc.tar.gz");
        final File output = getRenderDocInstallationPath().get().getAsFile();

        final CopyingFileTreeVisitor visitor = new CopyingFileTreeVisitor(output.toPath());
        getArchiveOperations().tarTree(tar).visit(visitor);
    }


}
