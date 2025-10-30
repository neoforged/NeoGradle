package net.neoforged.gradle.common.runtime.tasks;

import net.neoforged.gradle.common.extensions.problems.IProblemReporter;
import net.neoforged.gradle.dsl.common.tasks.Execute;
import org.gradle.api.GradleException;
import org.gradle.api.problems.Problems;
import org.gradle.api.tasks.CacheableTask;

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
            this.detector
        );
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
        if (detector.failed()) {
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

    private static final class OOMDetector extends OutputStream {
        private final ByteArrayOutputStream collectionDelegate = new ByteArrayOutputStream();
        private final StringBuilder resultBuilder = new StringBuilder();

        private OOMDetector() {
        }

        @Override
        public void write(final int b)
        {
            this.collectionDelegate.write(b);
        }

        @Override
        public void flush() throws IOException
        {
            this.collectionDelegate.flush();
        }

        @Override
        public void close() throws IOException
        {
            this.collectionDelegate.close();

            this.resultBuilder.append(this.collectionDelegate);
        }

        public boolean failed() {
            return this.resultBuilder.toString().contains("java.lang.OutOfMemoryError");
        }
    }
}
