package net.neoforged.gradle.dsl.platform.util

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.platform.model.Artifact
import net.neoforged.gradle.dsl.platform.model.Library
import net.neoforged.gradle.dsl.platform.model.LibraryDownload
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.model.ObjectFactory
import org.jetbrains.annotations.Nullable

import java.util.regex.Matcher
import java.util.regex.Pattern

@CompileStatic
abstract class ModuleIdentificationVisitor implements FileVisitor {

    // The following regex detects file path patterns, in gradle cache format. Like:  /net.neoforged.fancymodloader/earlydisplay/47.1.47/46509b19504a71e25b115383e900aade5088598a/earlydisplay-47.1.47.jar
    private static final Pattern GRADLE_CACHE_PATTERN = Pattern.compile('/(?<group>[^/]+)/(?<module>[^/]+)/(?<version>[^/]+)/(?<hash>[a-z0-9]+)/\\k<module>-\\k<version>(-(?<classifier>[^/]+))?\\.(?<extension>(jar)|(zip))$');

    // The following regex detects file path patterns, in maven local cache format. Like:  /.m2/repository/com/google/code/findbugs/jsr305/3.0.2/jsr305-3.0.2.jar
    private static final Pattern MAVEN_LOCAL_PATTERN = Pattern.compile('/.m2/repository/(?<group>.+)/(?<module>[^/]+)/(?<version>[^/]+)/\\k<module>-\\k<version>(-(?<classifier>[^/]+))?\\.(?<extension>(jar)|(zip))$');

    private final ObjectFactory objectFactory;

    ModuleIdentificationVisitor(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    void visitDir(FileVisitDetails dirDetails) {
        //Noop
    }

    @Override
    void visitFile(FileVisitDetails fileDetails) {
        final File file = fileDetails.getFile();
        final String absolutePath = file.getAbsolutePath().replace("\\", "/");

        Matcher matcher = GRADLE_CACHE_PATTERN.matcher(absolutePath);
        if (!matcher.find()) {
            matcher = MAVEN_LOCAL_PATTERN.matcher(absolutePath);
            if (!matcher.find()) {
                throw new IllegalStateException("The file " + file + " is not either a remove dependency or a maven local dependency!");
            }
        }

        final Library library = objectFactory.newInstance(Library.class);
        final LibraryDownload download = objectFactory.newInstance(LibraryDownload.class);
        final Artifact artifact = objectFactory.newInstance(Artifact.class);

        library.getDownload().set(download);
        download.getArtifact().set(artifact);

        final String group = matcher.group("group").replace("/", "."); //In case we match the maven way.
        final String module = matcher.group("module");
        final String version = matcher.group("version");
        final String classifier = matcher.group("classifier") == null ? "" : matcher.group("classifier");
        final String extension = matcher.group("extension")

        try {
            visitModule(file, group, module, version, classifier, extension);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void visitModule(final File file, final String group, final String module, final String version, @Nullable final String classifier, final String extension) throws Exception;
}
