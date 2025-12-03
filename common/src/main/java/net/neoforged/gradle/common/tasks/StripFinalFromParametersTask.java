package net.neoforged.gradle.common.tasks;

import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.dsl.common.tasks.WithOperations;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.tasks.specifications.InputFileSpecification;
import net.neoforged.gradle.util.ClassVisitingFileTreeVisitor;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class StripFinalFromParametersTask extends DefaultRuntime implements WithOutput, InputFileSpecification, WithOperations, WithWorkspace
{
    public StripFinalFromParametersTask() {
        super();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @TaskAction
    public void stripFinal() throws IOException {
        getCacheService().get()
            .cached(this,
                ICacheableJob.Default.file(this::doStripFinal, getOutput())
                )
            .execute();
    }

    private void doStripFinal() throws IOException {
        var input = getArchiveOperations().zipTree(getInput());
        try(final FileOutputStream fos = new FileOutputStream(ensureFileWorkspaceReady(getOutput()));
            final ZipOutputStream zos = new ZipOutputStream(fos)) {
            final ClassVisitingFileTreeVisitor visitor = new ClassVisitingFileTreeVisitor(zos, StripFinalClassVisitor::new);
            input.visit(visitor);
        }
    }

    private static class StripFinalClassVisitor extends ClassVisitor {
        public StripFinalClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new StripFinalParameterVisitor(api, mv);
        }
    }

    private static class StripFinalParameterVisitor extends MethodVisitor {
        public StripFinalParameterVisitor(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitParameter(String name, int access) {
            // Remove ACC_FINAL from parameter access flags
            int newAccess = access & ~Opcodes.ACC_FINAL;
            super.visitParameter(name, newAccess);
        }
    }
}