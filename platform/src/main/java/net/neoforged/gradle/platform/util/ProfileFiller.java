package net.neoforged.gradle.platform.util;

import net.neoforged.gradle.dsl.platform.model.Artifact;
import net.neoforged.gradle.dsl.platform.model.Library;
import net.neoforged.gradle.dsl.platform.model.LibraryDownload;
import net.neoforged.gradle.dsl.platform.util.ModuleIdentificationVisitor;
import net.neoforged.gradle.util.HashFunction;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class ProfileFiller extends ModuleIdentificationVisitor {
    
    private final ObjectFactory objectFactory;
    
    private final List<Library> libraries = new ArrayList<>();
    
    public ProfileFiller(ObjectFactory objectFactory) {
        super(objectFactory);
        this.objectFactory = objectFactory;
    }
    
    @Override
    protected void visitModule(File file, String group, String module, String version, @Nullable String classifier, final String extension) throws Exception {
        final Library library = objectFactory.newInstance(Library.class);
        final LibraryDownload download = objectFactory.newInstance(LibraryDownload.class);
        final Artifact artifact = objectFactory.newInstance(Artifact.class);
        
        library.getDownload().set(download);
        download.getArtifact().set(artifact);
        
        final String path = group.replace(".", "/") + "/" + module + "/" + version + "/" + module + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + extension;
        final String url = getMavenServerFor(path) + "/" + path;
        final String name = group + ":" + module + ":" + version + (classifier.isEmpty() ? "" : ":" + classifier) + "@" + extension;
        
        library.getName().set(name);
        try {
            artifact.getPath().set(path);
            artifact.getUrl().set(url);
            artifact.getSha1().set(HashFunction.SHA1.hash(file));
            artifact.getSize().set(Files.size(file.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        
        libraries.add(library);
    }
    
    private static String getMavenServerFor(String path) {
        try {
            final URL mojangMavenUrl = new URL("https://libraries.minecraft.net/" + path);
            final HttpURLConnection connection = (HttpURLConnection) mojangMavenUrl.openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getResponseCode() == 200 ? "https://libraries.minecraft.net" : "https://maven.neoforged.net/releases";
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            return "https://maven.neoforged.net/releases";
        }
    }
    
    public List<Library> getLibraries() {
        return libraries;
    }
}
