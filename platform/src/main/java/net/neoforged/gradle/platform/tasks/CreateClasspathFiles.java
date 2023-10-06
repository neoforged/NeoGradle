package net.neoforged.gradle.platform.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.common.runtime.tasks.DefaultRuntime;
import net.neoforged.gradle.platform.util.ArtifactPathsCollector;
import net.neoforged.gradle.platform.util.StringUtils;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CacheableTask
public abstract class CreateClasspathFiles extends DefaultRuntime implements TokenizedTask {
    
    
    public CreateClasspathFiles() {
        getTemplate().set(getProject().getRootProject().file("server_files/args.txt"));
        getOutputFileName().convention("args.txt");
    }
    
    @TaskAction
    public void doTask() throws Exception {
        final Map<String, Object> tokens = new HashMap<>(getTokens().get());
        
        ArtifactPathsCollector modulePathCollector = new ArtifactPathsCollector(getObjectFactory(), getPathSeparator().get(), "libraries/");
        ArtifactPathsCollector classpathCollector = new ArtifactPathsCollector(getObjectFactory(), getPathSeparator().get(), "libraries/");
        
        getModulePath().getAsFileTree().visit(modulePathCollector);
        getClasspath().getAsFileTree().visit(classpathCollector);
        
        tokens.put("MODULE_PATH", modulePathCollector.toString());
        
        final String externalClassPath = classpathCollector + getPathSeparator().get() +
                                                 String.format("libraries/net/minecraft/server/%s/server-%s-extra.jar", getNeoFormVersion().get(), getNeoFormVersion().get());
        final Set<String> claimedCoordinateLikePath = Arrays.stream(externalClassPath.split(getPathSeparator().get()))
                                                              .map(path -> StringUtils.getSlicedPrefixSection(path, "/", 2))
                                                              .collect(Collectors.toSet());
        
        final FileTree serverRawTree = getArchiveOperations().zipTree(getServer().get());
        final File joinedServerClasspathFile = serverRawTree.filter(spec -> {
            return spec.getPath().endsWith("META-INF/classpath-joined");
        }).getSingleFile();
        
        final String joinedServerClasspath = Files.readAllLines(joinedServerClasspathFile.toPath()).stream()
                                                     .flatMap(line -> Arrays.stream(line.split(";")))
                                                     .filter(path -> !claimedCoordinateLikePath.contains(StringUtils.getSlicedPrefixSection(path, "/", 2)))
                                                     .filter(path -> path.startsWith("libraries/"))
                                                     .collect(Collectors.joining(getPathSeparator().get()));
        
        final String classpath = externalClassPath + getPathSeparator().get() + joinedServerClasspath;
        tokens.put("CLASS_PATH", classpath);
        
        getFileSystemOperations().copy(copySpec -> {
            copySpec.from(getTemplate().get().getAsFile(), spec -> {
                spec.rename(name -> getOutputFileName().get());
                spec.filter(ImmutableMap.of("tokens", tokens), ReplaceTokens.class);
            });
            copySpec.into(getOutputDirectory().get().getAsFile());
        });
    }
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getTemplate();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getModulePath();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getClasspath();
    
    @Input
    public abstract Property<String> getPathSeparator();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServer();
    
    @Input
    public abstract Property<String> getNeoFormVersion();
}
