package net.neoforged.gradle.common.runtime.naming.tasks;

import net.neoforged.gradle.util.TransformerUtils;
import net.neoforged.gradle.common.runtime.naming.renamer.IMappingFileTypeRenamer;
import net.neoforged.gradle.common.runtime.naming.renamer.ITypeRenamer;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.minecraftforge.srgutils.IMappingFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CacheableTask
public abstract class UnapplyOfficialMappingsToAccessTransformer extends DefaultRuntime {

    public UnapplyOfficialMappingsToAccessTransformer() {
        super();

        getTypeRenamer().convention(
                getClientMappings().flatMap(clientMappings ->
                        getServerMappings().map(TransformerUtils.guard(serverMappings ->
                                IMappingFileTypeRenamer.from(
                                        IMappingFile.load(clientMappings.getAsFile()).reverse(),
                                        IMappingFile.load(serverMappings.getAsFile()).reverse()
                                ))))
        );
        getTypeRenamer().finalizeValueOnRead();
    }

    @TaskAction
    public void doUnApply() throws IOException {
        final List<String> lines = Files.readAllLines(getInput().getAsFile().get().toPath());

        final List<String> filterLines = lines.stream()
                .filter(line -> !line.startsWith("#"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> {
                    if (line.contains("#"))
                        return line.substring(0, line.indexOf('#')).trim();

                    return line;
                })
                .collect(Collectors.toList());

        final List<String> renamedLines = filterLines.stream()
                .map(this::renameLine)
                .collect(Collectors.toList());

        final File output = ensureFileWorkspaceReady(getOutput());
        Files.deleteIfExists(output.toPath());
        Files.write(output.toPath(), renamedLines, StandardOpenOption.CREATE_NEW);
    }

    private String renameLine(String line) {
        final ITypeRenamer typeRenamer = getTypeRenamer().get();

        final String[] parts = line.split(" ");
        final List<String> renamedParts = new ArrayList<>();
        renamedParts.add(parts[0]);
        renamedParts.add(parts[1]);
        if (parts.length > 2) {
            if (parts[2].contains("(")) {
                final String name = parts[2].substring(0, parts[2].indexOf('('));
                final String desc = parts[2].substring(parts[2].indexOf('('));
                renamedParts.add(typeRenamer.renameMethod(parts[1], name, desc) + desc);
            } else {
                renamedParts.add(typeRenamer.renameField(parts[1], parts[2]));
            }
        }

        return String.join(" ", renamedParts);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInput();

    @OutputFile
    public abstract RegularFileProperty getOutput();

    @Input
    public abstract Property<ITypeRenamer> getTypeRenamer();

    @Internal
    public abstract RegularFileProperty getClientMappings();

    @Internal
    public abstract RegularFileProperty getServerMappings();
}
