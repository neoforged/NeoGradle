package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.dsl.common.tasks.Execute;
import org.gradle.api.GradleException;
import org.gradle.api.problems.Problems;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@CacheableTask
public abstract class DecompilerExecute extends DefaultExecute
{

    private final OOMDetector detector = new OOMDetector();

    @Override
    public OutputStream createErrorOutputStream()
    {
        return new BifurcatingOutputStream(
            super.createErrorOutputStream(),
            new OOMDetectorStream(this.detector)
        );
    }

    @Internal
    public OOMDetector getDetector()
    {
        return detector;
    }

    @Override
    public void doExecute() throws Exception
    {
        try {
            super.doExecute();
        } catch (Exception ex) {
            detectError(false);
            throw ex;
        }

        detectError(true);
    }

    private void detectError(boolean throwError)
    {
        if (getDetector().failed()) {
            //We failed, so we can access the project now, to get the reporter
            //We should not need to care about the config cache here,
            getProject().getExtensions().getByType(IProblemReporter.class)
                .reporting(problem -> {
                    problem.contextualLabel("decompiler")
                        .id("decompiler", "memory")
                        .details("The Decompiler could not successfully decompile Minecraft because it ran out of memory. Modify your runtime configuration and the decompiler subsystems configuration, and try again.")
                        .solution("Either increase the memory allowance for the decompiler, or reduce the concurrency on the decompiler to reduce memory consumption.")
                        .section("common-decompiler-settings");
                }, getLogger());

            if (throwError) {
                throw new GradleException("The decompiler failed to run. It ran out of memory.");
            }
        }
    }

    public static final class OOMDetector {
        private final StringBuilder resultBuilder = new StringBuilder();

        private OOMDetector() {
        }

        private void addLog(String log) {
            resultBuilder.append(log);
        }

        public boolean failed() {
            return this.resultBuilder.toString().contains("java.lang.OutOfMemoryError");
        }
    }

    public static final class OOMDetectorStream extends OutputStream {
        private final OOMDetector detector;
        private final ByteArrayOutputStream collectionDelegate = new ByteArrayOutputStream();

        public OOMDetectorStream(final OOMDetector detector) {this.detector = detector;}

        @Override
        public void write(final int b) throws IOException
        {
            collectionDelegate.write(b);
        }

        @Override
        public void flush() throws IOException
        {
            super.flush();
            collectionDelegate.flush();
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            collectionDelegate.close();

            this.detector.addLog(collectionDelegate.toString());
        }
    }
}
