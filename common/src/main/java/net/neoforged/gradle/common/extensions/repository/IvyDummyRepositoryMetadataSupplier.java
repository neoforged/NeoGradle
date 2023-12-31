package net.neoforged.gradle.common.extensions.repository;

import com.google.common.collect.Lists;
import org.gradle.api.artifacts.ComponentMetadataBuilder;
import org.gradle.api.artifacts.ComponentMetadataSupplier;
import org.gradle.api.artifacts.ComponentMetadataSupplierDetails;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

public final class IvyDummyRepositoryMetadataSupplier implements ComponentMetadataSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(IvyDummyRepositoryMetadataSupplier.class);

    private final Provider<Set<IvyDummyRepositoryEntry>> extensionProvider;
    private final Provider<Directory> rootDirectoryProvider;

    @Inject
    public IvyDummyRepositoryMetadataSupplier(Provider<Set<IvyDummyRepositoryEntry>> extensionProvider, Provider<Directory> rootDirectoryProvider) {
        this.extensionProvider = extensionProvider;
        this.rootDirectoryProvider = rootDirectoryProvider;
    }

    @Override
    public void execute(ComponentMetadataSupplierDetails details) {
        final ModuleComponentIdentifier id = details.getId();
        final ComponentMetadataBuilder result = details.getResult();
        LOGGER.info("Preparing metadata for {}", id.getVersion());

        final Optional<IvyDummyRepositoryEntry> entryCandidate =
                extensionProvider.get().stream()
                        .filter(entry -> entry.matches(id))
                        .findFirst();

        if (!entryCandidate.isPresent()) {
            return;
        }

        try {
            final Path artifactPath = entryCandidate.get().buildArtifactPath(this.rootDirectoryProvider.get().getAsFile().toPath());
            Files.createFile(artifactPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dummy artifact!", e);
        }

        result.setStatus("Found");
        result.setStatusScheme(Lists.newArrayList("Found", "Not Found"));
    }
}
