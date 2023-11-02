package net.neoforged.gradle.platform.tasks;

import com.google.common.collect.ImmutableMap;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.dsl.common.tasks.WithWorkspace;
import org.apache.tools.ant.filters.ReplaceTokens;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Map;

@CacheableTask
public abstract class CreateLegacyInstaller extends Zip implements WithOutput, WithWorkspace, TokenizedTask {
    
    public CreateLegacyInstaller() {
        getArchiveClassifier().convention("installer-unsigned");
        getArchiveExtension().convention("jar");
        getDestinationDirectory().convention(getLayout().getBuildDirectory().dir("libs"));
        getArchiveFileName().convention(String.format("%s-%s-installer-unsigned.jar", this.getProject().getName(), this.getProject().getVersion()));
        getOutput().set(getArchiveFile());
        
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE);
        
        from(getInstallerCore().map(getArchiveOperations()::zipTree));
        from(getLauncherJson());
        from(getInstallerJson());
        from(getUrlIcon());
        exclude("big_logo.png");
        from(getInstallerLogo(), spec -> {
            spec.rename(original -> "big_logo.png");
        });
        
        from(getUnixServerArgs(), spec -> {
            spec.rename(name -> "data/unix_args.txt");
        });
        
        from(getWindowsServerArgs(), spec -> {
            spec.rename(name -> "data/win_args.txt");
        });
        
        from(getClientBinaryPatches(), spec -> {
            spec.into("data");
            spec.rename(name -> "client.lzma");
        });
        
        from(getServerBinaryPatches(), spec -> {
            spec.into("data");
            spec.rename(name -> "server.lzma");
        });
        
        getUrlIcon().fileValue(getProject().getRootProject().file("src/main/resources/url.png"));
        getInstallerLogo().fileValue(getProject().getRootProject().file("src/main/resources/neoforged_logo.png"));
        from(getData(), spec -> {
            spec.into("data");
            spec.filter(s -> {
                final Map<String, Object> tokens = getTokens().get();
                for (Map.Entry<String, Object> entry : tokens.entrySet()) {
                    s = s.replace(String.format("@%s@", entry.getKey()), entry.getValue().toString());
                }
                return s;
            });
        });
    }
    
    @Inject
    @Override
    public abstract @NotNull ObjectFactory getObjectFactory();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInstallerCore();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getClientBinaryPatches();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getServerBinaryPatches();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getLauncherJson();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInstallerJson();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getUrlIcon();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getInstallerLogo();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getUnixServerArgs();
    
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getWindowsServerArgs();
    
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    public abstract ConfigurableFileCollection getData();
}
