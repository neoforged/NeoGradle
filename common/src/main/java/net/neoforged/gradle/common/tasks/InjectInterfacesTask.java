package net.neoforged.gradle.common.tasks;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.services.caching.CachedExecutionService;
import net.neoforged.gradle.common.services.caching.jobs.ICacheableJob;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipOutputStream;

@CacheableTask
public abstract class InjectInterfacesTask extends DefaultRuntime implements WithOutput, InputFileSpecification, WithOperations, WithWorkspace {
    public InjectInterfacesTask() {
        super();
    }

    @ServiceReference(CachedExecutionService.NAME)
    public abstract Property<CachedExecutionService> getCacheService();

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getInterfaceInjectionFiles();

    @TaskAction
    public void injectInterfaces() throws IOException {
        getCacheService().get()
            .cached(this,
                ICacheableJob.Default.file(this::doInjectInterfaces, getOutput())
            )
            .execute();
    }

    private void doInjectInterfaces() throws IOException {
        Map<String, List<String>> injectionMap = parseInjectionFiles();
        var input = getArchiveOperations().zipTree(getInput());
        try (final FileOutputStream fos = new FileOutputStream(ensureFileWorkspaceReady(getOutput()));
             final ZipOutputStream zos = new ZipOutputStream(fos)) {
            final ClassVisitingFileTreeVisitor visitor = new ClassVisitingFileTreeVisitor(zos, (api, cv) -> new InjectInterfacesClassVisitor(api, cv, injectionMap));
            input.visit(visitor);
        }
    }

    private Map<String, List<String>> parseInjectionFiles() throws IOException {
        Map<String, List<String>> result = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (File file : getInterfaceInjectionFiles().getFiles()) {
            Map<String, Object> map = mapper.readValue(file, new TypeReference<>() {});
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                List<String> interfaces;
                if (entry.getValue() instanceof String) {
                    interfaces = List.of((String) entry.getValue());
                } else if (entry.getValue() instanceof List) {
                    interfaces = (List<String>) entry.getValue();
                } else {
                    throw new IllegalArgumentException("Invalid interface injection format in " + file + ": " + entry.getValue());
                }
                interfaces = interfaces.stream().map(s -> s.replace(".", "/")).toList();
                result.merge(entry.getKey(), interfaces, (a, b) -> { List<String> l = new ArrayList<>(a); l.addAll(b); return l; });
            }
        }
        return result;
    }

    @VisibleForTesting
    static class InjectInterfacesClassVisitor extends ClassVisitor {
        private final Map<String, List<String>> injectionMap;
        private List<String> interfacesToInject;

        public InjectInterfacesClassVisitor(int api, ClassVisitor classVisitor, Map<String, List<String>> injectionMap) {
            super(api, classVisitor);
            this.injectionMap = injectionMap;
        }

        @Override
        public void visit(int version, int access, String name, @Nullable String signature, String superName, String[] interfaces) {
            this.interfacesToInject = injectionMap.getOrDefault(name, Collections.emptyList());
            Set<String> all = new LinkedHashSet<>();
            Collections.addAll(all, interfaces);
            // For interface array, use only binary names (strip generics)
            all.addAll(interfacesToInject.stream().map(i -> i.contains("<") ? i.substring(0, i.indexOf('<')) : i).toList());
            String[] newInterfaces = all.toArray(new String[0]);
            if (signature == null && !interfacesToInject.isEmpty())
                signature = "";

            if (!interfacesToInject.isEmpty()) {
                var interfaceSignatures = new ArrayList<String>();
                // Add injected interfaces
                for (String iface : interfacesToInject) {
                    //Generic interface.
                    if (iface.contains("<")) {
                        // Parse generic parameters
                        //Handle the base type
                        StringBuilder interfaceSignature = createGenericSignature(iface);
                        interfaceSignatures.add(interfaceSignature.toString());
                    } else {
                        interfaceSignatures.add("L%s;".formatted(iface));
                    }
                }
                // Rebuild signature
                StringBuilder sb = new StringBuilder();
                sb.append(signature);
                for (String sig : interfaceSignatures) sb.append(sig);
                signature = sb.toString();
            }
            super.visit(version, access, name, signature, superName, newInterfaces);
        }

        private static StringBuilder createGenericSignature(final String iface)
        {
            StringBuilder interfaceSignature = new StringBuilder();

            interfaceSignature.append("L");

            interfaceSignature.append(
                iface,
                0, iface.indexOf('<')
            );

            interfaceSignature.append("<");

            String[] generics = iface.substring(iface.indexOf('<') + 1, iface.lastIndexOf('>')).split(",");
            for (final String generic : generics)
            {
                if (generic.contains("/")) {
                    interfaceSignature.append("L%s;".formatted(generic));
                } else {
                    interfaceSignature.append("T%s;".formatted(generic));
                }
            }

            interfaceSignature.append(">");

            interfaceSignature.append(";");

            return interfaceSignature;
        }
    }
}
