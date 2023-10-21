package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.dsl.common.runtime.tasks.NamedFiles;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeMultiArguments;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class RuntimeMultiArgumentsImpl implements RuntimeMultiArguments {
    
    private final ProviderFactory providerFactory;
    
    @Inject
    public RuntimeMultiArgumentsImpl(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }
    
    public Provider<List<String>> get(String key) {
        return getSimple().zip(getFiles(), (simple, files) -> {
            final List<String> result = simple.getOrDefault(key, new ArrayList<>());
            final Optional<NamedFiles> file = files.stream().filter(namedFile -> namedFile.getName().equals(key)).reduce((a, b) -> b);
            file.ifPresent(f -> result.addAll(f.getFiles().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList())));
            return result;
        });
    }
    
    public Provider<List<String>> getOrDefault(String key, Provider<List<String>> defaultProvider) {
        return get(key).orElse(defaultProvider);
    }
    
    public Provider<Map<String, Provider<List<String>>>> AsMap() {
        final Provider<Map<String, Provider<List<String>>>> simpleProvider =
                getSimple().map(Map::keySet)
                        .map(keySet -> keySet.stream().collect(Collectors.toMap(key -> key, key -> getSimple().getting(key))));

        
        final Provider<Map<String, Provider<List<String>>>> filesProvider =
                getFiles().map(files -> files.stream()
                                                .collect(Collectors.toMap(
                                                        NamedFiles::getName,
                                                        namedFiles -> providerFactory.provider(() -> namedFiles.getFiles().getFiles().stream().map(File::getAbsolutePath).collect(Collectors.toList())),
                                                        (a, b) -> b,
                                                        HashMap::new
                                                )));

        return simpleProvider.zip(filesProvider, (simple, files) -> ImmutableMap.<String, Provider<List<String>>>builder().putAll(simple).putAll(files).build());
    }
    
    @Override
    public void putFiles(String key, ConfigurableFileCollection provider) {
        getFiles().add(new NamedFiles(key, provider));
    }
    
    @Override
    public void putSimple(String patches, Provider<List<String>> provider) {
        getSimple().put(patches, provider);
    }
}
