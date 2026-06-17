package net.neoforged.gradle.common.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
import net.neoforged.gradle.common.util.EnumExtensionUtils;
import net.neoforged.gradle.dsl.common.tasks.WithOperations;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import net.neoforged.gradle.dsl.common.tasks.specifications.InputFileSpecification;
import net.neoforged.gradle.util.ClassVisitingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class ExtendEnumsTask extends DefaultRuntime implements WithOutput, InputFileSpecification, WithOperations, WithWorkspace {
    public ExtendEnumsTask() {
        super();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getEnumExtensionsFiles();
    
    @TaskAction
    public void extendEnums() throws IOException {
        getCacheService().get()
                .cached(this,
                        ICacheableJob.Default.file(this::doExtendEnums, getOutput())
                )
                .execute();
    }

    private void doExtendEnums() throws IOException {
        Map<String, Set<String>> extensionMap = parseExtensionFiles();
        var input = getArchiveOperations().zipTree(getInput());
        try (final FileOutputStream fos = new FileOutputStream(ensureFileWorkspaceReady(getOutput()));
             final ZipOutputStream zos = new ZipOutputStream(fos)) {
            final ClassVisitingFileTreeVisitor visitor = new ClassVisitingFileTreeVisitor(zos, (api, cv) -> new ExtendEnumsClassVisitor(api, cv, extensionMap));
            input.visit(visitor);
        }
    }

    private Map<String, Set<String>> parseExtensionFiles() throws IOException {
        var extensions = new HashMap<String, Set<String>>();
        ObjectMapper mapper = new ObjectMapper();
        for (var file : getEnumExtensionsFiles().getFiles()) {
            Map<String, Object> map = mapper.readValue(file, new TypeReference<>() {});
            var entries = map.get("entries");
            if (entries instanceof List<?> entryList) {
                for (var entry : entryList) {
                    if (entry instanceof Map<?, ?> entryMap) {
                        var enumName = entryMap.get("enum");
                        var entryName = entryMap.get("name");
                        if (!(enumName instanceof String enumNameString) || !(entryName instanceof String entryNameString)) {
                            throw new IllegalArgumentException("Invalid enum extension entry format in " + file + ": " + entry);
                        }
                        extensions.computeIfAbsent(enumNameString, unused -> new LinkedHashSet<>()).add(entryNameString);
                    } else {
                        throw new IllegalArgumentException("Invalid enum extension entry format in " + file + ": " + entry);
                    }
                }
            } else {
                throw new IllegalArgumentException("Invalid enum extension format in " + file + ": " + entries);
            }
        }
        return extensions;
    }

    @VisibleForTesting
    static class ExtendEnumsClassVisitor extends ClassVisitor {
        private final Map<String, Set<String>> extensionMap;
        private List<String> entries;
        private Type type;

        public ExtendEnumsClassVisitor(int api, ClassVisitor classVisitor, Map<String, Set<String>> extensionMap) {
            super(api, classVisitor);
            this.extensionMap = extensionMap;
        }

        @Override
        public void visit(int version, int access, String name, @Nullable String signature, String superName, String[] interfaces) {
            this.entries = extensionMap.getOrDefault(name, Collections.emptySet()).stream().sorted().toList();
            this.type = Type.getObjectType(name);
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitEnd() {
            for (String entry : entries) {
                var fieldVisitor = visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, entry, type.getDescriptor(), null, null);
                fieldVisitor.visitAnnotation(Type.getObjectType(EnumExtensionUtils.MARKER_ANNOTATION).getDescriptor(), false).visitEnd();
                fieldVisitor.visitEnd();
            }
            super.visitEnd();
        }
    }
}
