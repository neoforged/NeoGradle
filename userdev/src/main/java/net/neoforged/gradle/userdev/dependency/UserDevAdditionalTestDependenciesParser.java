package net.neoforged.gradle.userdev.dependency;

import net.neoforged.gradle.dsl.userdev.configurations.UserdevProfile;
import net.neoforged.gradle.util.TransformerUtils;
import org.gradle.api.Project;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.FileTree;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UserDevAdditionalTestDependenciesParser {

    final Project project;

    public UserDevAdditionalTestDependenciesParser(Project project) {
        this.project = project;
    }

    public Provider<List<String>> parse(File file) {
        if (!file.exists())
            return project.provider(Collections::emptyList);

        try {
            return parseFileInternal(file);
        } catch (Exception e) {
            return project.provider(Collections::emptyList);
        }
    }

    private Provider<List<String>> parseFileInternal(File file) {
        final FileTree fileTree = file.getName().endsWith(".jar") || file.getName().endsWith(".zip") ?
                project.zipTree(file) :
                project.fileTree(file);

        final var providers = fileTree.matching(pattern -> pattern.include("config.json"))
                .getElements()
                .map(fls -> fls.stream()
                        .map(FileSystemLocation::getAsFile)
                        .filter(File::isFile)
                        .toList()
                )
                .map(files -> files.stream()
                        .map(this::parseInternalFile)
                        .collect(Collectors.toList()));

        return providers.flatMap(TransformerUtils.combineAllLists(project, String.class, Function.identity()));
    }

    private Provider<List<String>> parseInternalFile(File file) {
        try(final FileInputStream inputStream = new FileInputStream(file)) {
            return UserdevProfile.get(project.getObjects(), inputStream)
                    .getAdditionalTestDependencyArtifactCoordinates();
        } catch (Exception e) {
            return project.provider(Collections::emptyList);
        }
    }
}
