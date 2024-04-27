package net.neoforged.gradle.common.runtime.naming.tasks;

import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.util.FileUtils;
import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.common.util.CacheableIMappingFile;
import net.neoforged.gradle.dsl.common.extensions.MinecraftArtifactCache;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipOutputStream;

@NotNull
public abstract class GenerateDebuggingMappings extends DefaultRuntime {

    public GenerateDebuggingMappings() {
        getMappingsFile().convention(getMinecraftVersion()
                .map(minecraftVersion -> getProject().getExtensions().getByType(MinecraftArtifactCache.class)
                        .cacheVersionMappings(minecraftVersion, DistributionType.CLIENT))
                .map(TransformerUtils.guard(IMappingFile::load))
                .map(CacheableIMappingFile::new));

        getOutputFileName().convention("mappings.zip");
    }

    @TaskAction
    public void generate() throws Exception {
        final File output = ensureFileWorkspaceReady(getOutput());

        IMappingFile mappingFile = getMappingsFile().get();

        Map<String, String> fieldMappings = new TreeMap<>();
        Map<String, String> methodMappings = new TreeMap<>();

        for (IMappingFile.IClass cls : mappingFile.getClasses()) {
            for (IMappingFile.IField fld : cls.getFields()) {
                String srgFieldName = fld.getOriginal();
                if (srgFieldName.startsWith("field_") || srgFieldName.startsWith("f_"))
                    fieldMappings.put(srgFieldName, fld.getMapped());
            }
            for (IMappingFile.IMethod mtd : cls.getMethods()) {
                String srgMethodName = mtd.getOriginal();
                if (srgMethodName.startsWith("func_") || srgMethodName.startsWith("m_"))
                    methodMappings.put(srgMethodName, mtd.getMapped());
            }
        }

        String[] header = new String[]{"searge", "name", "side", "desc"};
        List<String[]> fields = new ArrayList<>();
        List<String[]> methods = new ArrayList<>();
        fields.add(header);
        methods.add(header);

        for (String name : fieldMappings.keySet()) {
            String cname = fieldMappings.get(name);
            fields.add(new String[]{name, cname, "2", ""});
        }

        for (String name : methodMappings.keySet()) {
            String cname = methodMappings.get(name);
            methods.add(new String[]{name, cname, "2", ""});
        }

        try (FileOutputStream fos = new FileOutputStream(output);
             ZipOutputStream out = new ZipOutputStream(fos)) {
            FileUtils.addCsvToZip("fields.csv", fields, out);
            FileUtils.addCsvToZip("methods.csv", methods, out);
        }
    }

    @Input
    public abstract Property<CacheableIMappingFile> getMappingsFile();
}
