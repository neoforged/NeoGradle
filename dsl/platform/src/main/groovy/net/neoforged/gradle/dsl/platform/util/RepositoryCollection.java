package net.neoforged.gradle.dsl.platform.util;

import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.ProviderFactory;

import java.net.URI;

public class RepositoryCollection {
    private final ListProperty<URI> urls;

    public RepositoryCollection(ProviderFactory providers, ObjectFactory objects, RepositoryHandler handler) {
        this.urls = objects.listProperty(URI.class);
        handler.withType(MavenArtifactRepository.class).configureEach(repo -> urls.add(providers.provider(repo::getUrl).map(RepositoryCollection::addTrailingSlash)));
    }

    public ListProperty<URI> getURLs() {
        return urls;
    }

    private static URI addTrailingSlash(URI uri) {
        String asString = uri.toString();
        if (asString.endsWith("/")) return uri;
        return URI.create(asString + "/");
    }
}
