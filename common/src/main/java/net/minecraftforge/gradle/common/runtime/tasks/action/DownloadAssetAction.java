package net.minecraftforge.gradle.common.runtime.tasks.action;

import net.minecraftforge.gradle.common.runtime.tasks.DownloadAssets;
import net.minecraftforge.gradle.common.util.FileUtils;
import net.minecraftforge.gradle.common.util.GradleInternalUtils;
import net.minecraftforge.gradle.common.util.HashFunction;
import net.minecraftforge.gradle.common.util.UrlUtils;
import net.minecraftforge.gradle.common.util.workers.DefaultWorkerExecutorHelper;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressEvent;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.provider.Property;
import org.omg.CORBA.TIMEOUT;

import java.net.URL;

public abstract class DownloadAssetAction {

    private final Logger logger;
    private final Object servicesOwner;
    private final boolean isOffline;

    public DownloadAssetAction(DownloadAssets task) {
        this.logger = task.getProject().getLogger();
        this.servicesOwner = task;
        this.isOffline = task.getProject().getGradle().getStartParameter().isOffline();

        getShouldValidateHash().convention(true);
    }

    public void execute(final DefaultWorkerExecutorHelper helper) {
        helper.submit(() -> {
            try {
                final GradleInternalUtils.ProgressLoggerWrapper progress = GradleInternalUtils.getProgressLogger(this.logger, this.servicesOwner, "Download Asset: " + this.getUrl().get());
                progress.setDestFileName(getOutputFile().getAsFile().get().getName());

                if (isOffline) {
                    if (!getOutputFile().get().getAsFile().exists()) {
                        throw new IllegalStateException("Cannot download asset " + getUrl().get() + " as Gradle is running in offline mode and the file does not exist");
                    }

                    final int size = FileUtils.getFileSize(getOutputFile().get().getAsFile());
                    progress.setSize(size);
                    progress.started();

                    if (getShouldValidateHash().get()) {
                        final String hash = HashFunction.SHA1.hash(getOutputFile().get().getAsFile());
                        if (!hash.equals(getSha1().get())) {
                            throw new IllegalStateException("Cannot validate asset " + getUrl().get() + " as Gradle is running in offline mode and the file does not match the expected hash");
                        }
                    }

                    progress.incrementDownloadProgress(size);
                    progress.completed();
                    return;
                }

                final URL url = new URL(getUrl().get());

                progress.setSize(UrlUtils.getFileSize(url));

                FileUtil.copy(
                        new URL(getUrl().get()),
                        getOutputFile().get().getAsFile(),
                        new Monitor(progress),
                        Timeout.NONE
                );

                if (getShouldValidateHash().get()) {
                    final String hash = HashFunction.SHA1.hash(getOutputFile().get().getAsFile());
                    if (!hash.equals(getSha1().get())) {
                        throw new IllegalStateException("Cannot validate asset " + getUrl().get() + " as Gradle is running in offline mode and the file does not match the expected hash");
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public abstract Property<String> getUrl();

    public abstract Property<String> getSha1();

    public abstract Property<Boolean> getShouldValidateHash();

    public abstract RegularFileProperty getOutputFile();

    private static final class Monitor implements CopyProgressListener {
        private final GradleInternalUtils.ProgressLoggerWrapper progress;

        private Monitor(GradleInternalUtils.ProgressLoggerWrapper progress) {
            this.progress = progress;
        }

        @Override
        public void start(CopyProgressEvent evt) {
            progress.started();
            progress.incrementDownloadProgress(evt.getReadBytes());
        }

        @Override
        public void progress(CopyProgressEvent evt) {
            progress.incrementDownloadProgress(evt.getReadBytes());
        }

        @Override
        public void end(CopyProgressEvent evt) {
            progress.incrementDownloadProgress(evt.getReadBytes());
            progress.completed();
        }
    }

    private static final class Timeout implements TimeoutConstraint {

        private static final Timeout NONE = new Timeout();

        @Override
        public int getConnectionTimeout() {
            return -1;
        }

        @Override
        public int getReadTimeout() {
            return -1;
        }
    }
}
