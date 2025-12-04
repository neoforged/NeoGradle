package net.neoforged.gradle.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.zip.ZipOutputStream;


public class ClassVisitingFileTreeVisitor extends AdaptingZipBuildingFileTreeVisitor
{
    public ClassVisitingFileTreeVisitor(final ZipOutputStream outputZipStream, VisitorBuilder builder)
    {
        super(outputZipStream, (details, outputStream) -> {
            if (!details.getName().endsWith(".class"))
            {
                details.copyTo(outputStream);
                return;
            }

            try (InputStream stream = details.open()) {
                ClassReader cr = new ClassReader(stream);
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                ClassVisitor cv = builder.build(Opcodes.ASM9, cw);
                cr.accept(cv, 0);
                outputStream.write(cw.toByteArray());
            }
        });
    }

    public interface VisitorBuilder {
        ClassVisitor build(int apiVersion, ClassVisitor sink);
    }
}
