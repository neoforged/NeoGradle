package net.neoforged.gradle.common.extensions.repository;

import com.google.common.collect.Lists;
import net.neoforged.gradle.dsl.common.extensions.repository.Entry;
import org.gradle.api.artifacts.*;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Optional;
import java.util.Set;

public class IvyMetadata implements ComponentMetadataSupplier {

    private final Provider<Set<MetadataEntry>> repoEntries;

    /**
     * Constructor
     * @param repoEntries This needs to be a provider, because this needs to be isolated from the actual entries.
     */
    @Inject
    public IvyMetadata(Provider<Set<MetadataEntry>> repoEntries) {
        this.repoEntries = repoEntries;
    }

    @Override
    public void execute(ComponentMetadataSupplierDetails details) {
        final ModuleComponentIdentifier id = details.getId();
        final ComponentMetadataBuilder result = details.getResult();

        final Optional<MetadataEntry> entryCandidate =
                repoEntries.get().stream()
                        .filter(metadataEntry -> metadataEntry.matches(id))
                        .findFirst();

        if (!entryCandidate.isPresent()) {
            return;
        }

        result.setStatus("Found");
        result.setStatusScheme(Lists.newArrayList("Found", "Not Found"));
    }

    public static final class MetadataEntry implements Serializable {

        public static MetadataEntry from(Entry entry) {
            return new MetadataEntry(entry.getDependency().getGroup(), entry.getDependency().getName(), entry.getDependency().getVersion());
        }

        private final String group;
        private final String name;
        private final String version;

        private MetadataEntry(String group, String name, String version) {
            this.group = group;
            this.name = name;
            this.version = version;
        }

        private boolean matches(ModuleComponentIdentifier id) {
            return group.equals(id.getGroup())
                    && name.equals(id.getModule())
                    && version.equals(id.getVersion());
        }
    }
}
