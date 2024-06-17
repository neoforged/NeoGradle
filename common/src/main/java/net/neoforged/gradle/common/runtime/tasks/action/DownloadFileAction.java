package net.neoforged.gradle.common.runtime.tasks.action;

import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.GradleInternalUtils;
import net.neoforged.gradle.util.HashFunction;
import net.neoforged.gradle.util.UrlUtils;
import org.apache.ivy.core.settings.TimeoutConstraint;
import org.apache.ivy.util.CopyProgressEvent;
import org.apache.ivy.util.CopyProgressListener;
import org.apache.ivy.util.FileUtil;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildServiceRegistry;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import javax.inject.Inject;
import java.io.File;
import java.net.URL;

public abstract class DownloadFileAction implements WorkAction<DownloadFileAction.Params> {
    private static final int MAX_ATTEMPTS = 10;
    private static final int ATTEMPT_DELAY = 100;
    private static final Logger LOGGER = Logging.getLogger(DownloadFileAction.class);

    @Inject
    public abstract BuildServiceRegistry getBuildServiceRegistry();

    @Override
    public void execute() {
        try {
            final Params params = getParameters();
            final File output = params.getOutputFile().get().getAsFile();

            if (output.exists()) {
                if (params.getShouldValidateHash().get()) {
                    final String hash = HashFunction.SHA1.hash(output);
                    if (hash.equals(params.getSha1().get())) {
                        return;
                    }
                }
            }

            final GradleInternalUtils.ProgressLoggerWrapper progress = GradleInternalUtils.getProgressLogger(LOGGER, getBuildServiceRegistry(), "Downloading file: " + params.getUrl().get());
            progress.setDestFileName(params.getOutputFile().getAsFile().get().getName());

            if (params.getIsOffline().get()) {
                if (!output.exists()) {
                    throw new IllegalStateException("Cannot download asset " + params.getUrl().get() + " as Gradle is running in offline mode and the file does not exist");
                }

                final int size = FileUtils.getFileSize(output);
                progress.setSize(size);
                progress.started();

                if (params.getShouldValidateHash().get()) {
                    final String hash = HashFunction.SHA1.hash(output);
                    if (!hash.equals(params.getSha1().get())) {
                        throw new IllegalStateException(String.format("Cannot validate asset %s as Gradle is running in offline mode and the file does not match the expected hash. Expected: %s Actual: %s", params.getUrl().get(), params.getSha1().get(), hash));
                    }
                }

                progress.incrementDownloadProgress(size);
                progress.completed();
                return;
            }

            final URL url = new URL(params.getUrl().get());

            // Try downloading multiple times with a small delay in case of blocked connections
            for (int attempt = 0; attempt < MAX_ATTEMPTS; ++attempt) {
                try {
                    progress.setSize(UrlUtils.getFileSize(url));

                    FileUtil.copy(
                            url,
                            output,
                            new Monitor(progress),
                            Timeout.NONE
                    );

                    break; // Success
                } catch (Exception e) {
                    if (attempt == MAX_ATTEMPTS - 1) {
                        throw e;
                    }

                    try {
                        Thread.sleep(ATTEMPT_DELAY);
                    } catch (InterruptedException interruptedException) {
                        throw new RuntimeException(interruptedException);
                    }
                }
            }

            if (params.getShouldValidateHash().get()) {
                final String hash = HashFunction.SHA1.hash(output);
                if (!hash.equals(params.getSha1().get())) {
                    throw new IllegalStateException(String.format("Cannot validate asset %s as Gradle is running in offline mode and the file does not match the expected hash. Expected: %s Actual: %s", params.getUrl().get(), params.getSha1().get(), hash));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private record Monitor(GradleInternalUtils.ProgressLoggerWrapper progress) implements CopyProgressListener {

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

    public interface Params extends WorkParameters {
        Property<String> getUrl();
        Property<String> getSha1();
        Property<Boolean> getShouldValidateHash();
        RegularFileProperty getOutputFile();
        Property<Boolean> getIsOffline();
    }
}
