package net.neoforged.gradle.common.runtime.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.dsl.common.runtime.tasks.*;
import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class RuntimeArgumentsImpl implements RuntimeArguments {

    private final ProviderFactory providerFactory;
    
    @Inject
    public RuntimeArgumentsImpl(ProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }
    
    public Provider<String> get(String key) {
        return getSimple().zip(getFiles(), (simple, files) -> {
            final Optional<NamedFileRef> namedFile = files.stream().filter(f -> f.getName().equals(key)).findFirst();
            
            if (simple.containsKey(key) && namedFile.isPresent()) {
                throw new IllegalArgumentException("Cannot have both a simple and file argument for key: " + key);
            } else if (simple.containsKey(key)) {
                return simple.get(key);
            } else {
                return namedFile.map(file -> file.getFile().getAbsolutePath()).orElse(null);
            }
        });
    }
    
    public Provider<String> getOrDefault(String key, Provider<String> defaultProvider) {
        return get(key).orElse(defaultProvider);
    }
    
    public Provider<Map<String, Provider<String>>> asMap() {
        final Provider<Map<String, Provider<String>>> simpleProvider =
                getSimple().map(Map::keySet)
                        .map(keySet -> keySet.stream().collect(Collectors.toMap(key -> key, key -> getSimple().getting(key))));
        
        final Provider<Map<String, Provider<String>>> filesProvider =
                getFiles().map(files -> files.stream()
                                                .collect(Collectors.toMap(
                                                        NamedFileRef::getName,
                                                        namedFiles -> providerFactory.provider(() -> namedFiles.getFile().getAbsolutePath()),
                                                        (a, b) -> b,
                                                        HashMap::new
                                                )));
                
        return simpleProvider.zip(filesProvider, (simple, files) -> ImmutableMap.<String, Provider<String>>builder().putAll(simple).putAll(files).build());
    }
    
    @Override
    public void putFile(String input, Provider<File> fileProvider) {
        getFiles().add(fileProvider.map(file -> new NamedFile(input, file)));
    }
    
    @Override
    public void putRegularFile(String input, Provider<RegularFile> fileProvider) {
        getFiles().add(new NamedRegularFile(input, fileProvider));
    }
    
    @Override
    public void putDirectoryFile(String input, Provider<File> fileProvider) {
        getFiles().add(fileProvider.map(file -> new NamedDirectoryFile(input, file)));
    }
    
    @Override
    public void putDirectory(String input, Provider<Directory> fileProvider) {
        getFiles().add(new NamedDirectory(input, fileProvider));
    }
    
    @Override
    public void put(String input, Provider<String> stringProvider) {
        getSimple().put(input, stringProvider);
    }
}
