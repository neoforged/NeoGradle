package net.neoforged.gradle.platform.tasks;

import net.neoforged.gradle.util.CopyingFileTreeVisitor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.*;

import java.io.File;

@CacheableTask
public abstract class SetupProjectFromRuntime extends DefaultTask {
    
    public SetupProjectFromRuntime() {
        super();
        
        setGroup("setup");
        
        getSourcesDirectory().convention(
                getProject().getLayout().dir(
                        getProject().provider(() -> {
                            final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
                            final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
                            return mainSource.getJava().getFiles().size() == 1 ? mainSource.getJava().getSourceDirectories().getSingleFile() : getProject().file("src/main/java");
                        })
                )
        );
        
        getResourcesDirectory().convention(
                getProject().getLayout().dir(
                        getProject().provider(() -> {
                            final JavaPluginExtension javaPluginExtension = getProject().getExtensions().getByType(JavaPluginExtension.class);
                            final SourceSet mainSource = javaPluginExtension.getSourceSets().getByName("main");
                            return mainSource.getResources().getSourceDirectories().getSingleFile();
                        })
                )
        );
    }
    
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @TaskAction
    public void doSetup() throws Exception {
        final File sourceFile = getSourcesFile().get().getAsFile();
        final FileTree jarFileTree = getProject().zipTree(sourceFile);
        
        final FileTree codeFiles = jarFileTree.matching(filter -> filter.include("**/**.java"));
        final FileTree noneCodeFiles = jarFileTree.matching(filter -> filter.exclude("**/**.java"));
        
        final File sourceDirectory = getSourcesDirectory().get().getAsFile();
        final File resourcesDirectory = getResourcesDirectory().get().getAsFile();
        
        codeFiles.visit(new CopyingFileTreeVisitor(sourceDirectory, false));
        noneCodeFiles.visit(new CopyingFileTreeVisitor(resourcesDirectory, false));
        
        sourceDirectory.setReadOnly();
        resourcesDirectory.setReadOnly();
    }
    
    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract RegularFileProperty getSourcesFile();
    
    @OutputDirectory
    public abstract DirectoryProperty getSourcesDirectory();
    
    @OutputDirectory
    public abstract DirectoryProperty getResourcesDirectory();
}
