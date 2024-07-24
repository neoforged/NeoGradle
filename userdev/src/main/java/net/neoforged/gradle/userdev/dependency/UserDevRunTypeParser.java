package net.neoforged.gradle.userdev.dependency;

import net.neoforged.gradle.dsl.common.runs.type.RunType;
import net.neoforged.gradle.dsl.common.runs.type.RunTypeManager;
import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import org.gradle.api.Project;
import org.gradle.api.file.FileTree;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class UserDevRunTypeParser implements RunTypeManager.Parser {

    private final Project project;

    public UserDevRunTypeParser(Project project) {
        this.project = project;
    }

    @Override
    public Collection<RunType> parse(File file) {
        if (!file.exists())
            return List.of();

        try {
            return parseInternal(file);
        } catch (Exception e) {
            return List.of();
        }
    }

    private @NotNull List<RunType> parseInternal(File file) {
        final FileTree fileTree = file.getName().endsWith(".jar") || file.getName().endsWith(".zip") ?
                project.zipTree(file) :
                project.fileTree(file);
        return fileTree.matching(pattern -> pattern.include("config.json"))
                .getFiles()
                .stream()
                .flatMap(this::parseInternalFile)
                .toList();
    }

    private Stream<RunType> parseInternalFile(File file) {
        try(final FileInputStream inputStream = new FileInputStream(file)) {
            return UserdevProfile.get(project.getObjects(), inputStream)
                    .getRunTypes().stream();
        } catch (IOException e) {
            return Stream.empty();
        }
    }
}
