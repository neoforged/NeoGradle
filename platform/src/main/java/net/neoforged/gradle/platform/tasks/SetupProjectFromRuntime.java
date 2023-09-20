package net.neoforged.gradle.platform.tasks;

import net.neoforged.gradle.dsl.common.tasks.WithOperations;
import net.neoforged.gradle.platform.util.SetupUtils;
import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class SetupProjectFromRuntime extends DefaultTask implements WithOperations {
    
    public SetupProjectFromRuntime() {
        super();
        
        setGroup("setup");
        
        final File defaultSourceTarget = SetupUtils.getSetupSourceTarget(getProject());
        final File defaultResourceTarget = SetupUtils.getSetupResourcesTarget(getProject());
        
        getSourcesDirectory().convention(
                getProject().getLayout().dir(getProviderFactory().provider(() -> defaultSourceTarget))
        );
        
        getResourcesDirectory().convention(
                getProject().getLayout().dir(getProviderFactory().provider(() -> defaultResourceTarget))
        );
        
        getShouldLockDirectories().convention(true);
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @TaskAction
    public void doSetup() throws Exception {
        final File sourceFile = getSourcesFile().get().getAsFile();
        final FileTree jarFileTree = getArchiveOperations().zipTree(sourceFile);
        
        final FileTree codeFiles = jarFileTree.matching(filter -> filter.include("**/**.java"));
        final FileTree noneCodeFiles = jarFileTree.matching(filter -> filter.exclude("**/**.java"));
        
        final File sourceDirectory = getSourcesDirectory().get().getAsFile();
        final File resourcesDirectory = getResourcesDirectory().get().getAsFile();
        
        sourceDirectory.setWritable(true);
        resourcesDirectory.setWritable(true);
        
        codeFiles.visit(new CopyingFileTreeVisitor(sourceDirectory, false));
        noneCodeFiles.visit(new CopyingFileTreeVisitor(resourcesDirectory, false));
        
        if (getShouldLockDirectories().get()) {
            sourceDirectory.setReadOnly();
            resourcesDirectory.setReadOnly();
        }
    }
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourcesFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getSourcesDirectory();
    
    @OutputDirectory
    public abstract DirectoryProperty getResourcesDirectory();
    
    @Input
    @Optional
    public abstract Property<Boolean> getShouldLockDirectories();
}
