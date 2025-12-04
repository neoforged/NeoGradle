package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.dsl.common.runtime.tasks.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class RuntimeArgumentsImpl implements RuntimeArguments
{
    @Nullable
    public Provider<String> get(String key)
    {
        return
            getFiles().map(files -> files.stream().filter(f -> f.getName().equals(key)).findFirst().orElse(null))
                .flatMap(NamedFileRef::getFile)
                .map(File::getAbsolutePath)
                .orElse(getSimple().getting(key));
    }

    public Provider<String> getOrDefault(String key, Provider<String> defaultProvider)
    {
        final Provider<String> getResult = get(key);
        if (getResult == null)
        {
            return defaultProvider;
        }

        return getResult.orElse(defaultProvider);
    }

    public Provider<Map<String, Provider<String>>> asMap()
    {
        final Provider<Map<String, Provider<String>>> simpleProvider =
            getSimple().map(Map::keySet)
                .map(keySet -> keySet.stream().collect(Collectors.toMap(key -> key, key -> getSimple().getting(key))));

        final Provider<Map<String, Provider<String>>> filesProvider =
            getFiles().map(files -> files.stream()
                .collect(Collectors.toMap(
                    NamedFileRef::getName,
                    namedFiles -> namedFiles.getFile().map(File::getAbsolutePath),
                    (a, b) -> b,
                    HashMap::new
                )));

        return simpleProvider.zip(filesProvider, (simple, files) -> ImmutableMap.<String, Provider<String>>builder().putAll(simple).putAll(files).build());
    }

    @Override
    public void putFile(String input, Provider<File> fileProvider)
    {
        getFiles().add(new NamedFile(input, fileProvider));
    }

    @Override
    public void putRegularFile(String input, Provider<RegularFile> fileProvider)
    {
        getFiles().add(new NamedRegularFile(input, fileProvider));
    }

    @Override
    public void put(String input, Provider<String> stringProvider)
    {
        getSimple().put(input, stringProvider);
    }
}
