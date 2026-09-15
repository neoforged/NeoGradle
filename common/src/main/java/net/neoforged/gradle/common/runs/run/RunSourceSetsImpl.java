package net.neoforged.gradle.common.runs.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gdi.annotations.DSLProperty;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.runs.run.RunSourceSets;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public abstract class RunSourceSetsImpl implements RunSourceSets {

    private final Project project;
    private final Multimap<String, SourceSet> sourceSets;
    private final List<Action<SourceSet>> callbacks = new ArrayList<>();
    private final List<Provider<Multimap<String, SourceSet>>> sourceSetProviders = new ArrayList<>();
    private final List<LazyModSourceRef> lazyModSources = new ArrayList<>();

    @Inject
    public RunSourceSetsImpl(Project project) {
        this.project = project;
        this.sourceSets = HashMultimap.create();
    }


    @Override
    public void add(SourceSet sourceSet) {
        this.sourceSets.put(SourceSetUtils.getModIdentifier(sourceSet, null), sourceSet);

        for (Action<SourceSet> callback : callbacks) {
            callback.execute(sourceSet);
        }
    }

    /**
     * Registers a lazy mod source reference by project path and source set name.
     * Resolution happens at execution time via Gradle's service injection to avoid cross-project access during configuration.
     */
    @Override
    public void addLazy(String modSourceKey, String groupId) {
        this.lazyModSources.add(new LazyModSourceRef(modSourceKey, groupId));
    }

    @Override
    public void add(Iterable<? extends SourceSet> sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            add(sourceSet);
        }
    }

    @Override
    public void add(SourceSet... sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            add(sourceSet);
        }
    }

    @Override
    public void local(SourceSet sourceSet) {
        this.sourceSets.put(SourceSetUtils.getModIdentifier(sourceSet, project), sourceSet);

        for (Action<SourceSet> callback : callbacks) {
            callback.execute(sourceSet);
        }
    }

    @Override
    public void local(Iterable<? extends SourceSet> sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            local(sourceSet);
        }
    }

    @Override
    public void local(SourceSet... sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            local(sourceSet);
        }
    }

    @Override
    public void add(String groupId, SourceSet sourceSet) {
        this.sourceSets.put(groupId, sourceSet);

        for (Action<SourceSet> callback : callbacks) {
            callback.execute(sourceSet);
        }
    }

    @Override
    public void add(String groupId, Iterable<? extends SourceSet> sourceSets) {
        this.sourceSets.putAll(groupId, sourceSets);

        for (SourceSet sourceSet : sourceSets) {
            for (Action<SourceSet> callback : callbacks) {
                callback.execute(sourceSet);
            }
        }
    }

    @Override
    public void add(String groupId, SourceSet... sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            add(groupId, sourceSet);
        }
    }

    @Override
    public void addAllLater(Provider<Multimap<String, SourceSet>> sourceSets) {
        this.sourceSetProviders.add(sourceSets);
    }

    @DSLProperty
    @Input
    @Optional
    @Override
    public abstract Property<SourceSet> getPrimary();

    @Override
    public Provider<Multimap<String, SourceSet>> all() {
        //Realize all lazy source sets from providers
        if (!this.sourceSetProviders.isEmpty()) {
            final var providers = new ArrayList<>(this.sourceSetProviders);
            this.sourceSetProviders.clear();
            for (Provider<Multimap<String, SourceSet>> sourceSetProvider : providers) {
                final Multimap<String, SourceSet> sourceSets = sourceSetProvider.get();
                sourceSets.forEach(this::add);
            }
        }

        // Note: Lazy mod sources from string references are NOT resolved here.
        // They must be resolved via resolveLazyModSources(Project rootProject) which is called
        // at task execution time with an injected RootProject to avoid isolated projects violations.

        return this.project.provider(() -> this.sourceSets);
    }

    /**
     * Resolves lazy mod source string references using the provided root project.
     * This MUST be called at task execution time (not configuration time) to avoid isolated projects violations.
     */
    public static void resolveLazyModSources(RunSourceSets runSourceSets, Project rootProject) {
        if (!(runSourceSets instanceof RunSourceSetsImpl impl)) {
            return;
        }

        if (impl.lazyModSources.isEmpty()) {
            return; // Nothing to resolve
        }

        for (LazyModSourceRef ref : impl.lazyModSources) {
            SourceSet resolved = impl.resolveModSourceFromRoot(ref.modSourceKey, rootProject);
            String groupId = ref.groupId != null ? ref.groupId : 
                extractProjectPathFromKey(ref.modSourceKey);
            impl.sourceSets.put(groupId, resolved);

            for (Action<SourceSet> callback : impl.callbacks) {
                callback.execute(resolved);
            }
        }
        impl.lazyModSources.clear();
    }

    /**
     * Resolves a mod source key to an actual SourceSet using the provided root project.
     */
    private SourceSet resolveModSourceFromRoot(String modSourceKey, Project rootProject) {
        int colonIndex = modSourceKey.lastIndexOf(':');
        if (colonIndex == -1) {
            throw new org.gradle.api.GradleException(
                "Invalid mod source key: '" + modSourceKey + "'. Expected format: 'projectPath:sourceSetName'");
        }

        String projectPath = modSourceKey.substring(0, colonIndex);
        String sourceSetName = modSourceKey.substring(colonIndex + 1);

        Project targetProject = rootProject.findProject(projectPath);
        if (targetProject == null) {
            throw new org.gradle.api.GradleException(
                "Could not find project '" + projectPath + "' for mod source key: '" + modSourceKey + "'");
        }

        SourceSetContainer sourceSets = targetProject.getExtensions().getByType(SourceSetContainer.class);
        SourceSet sourceSet = sourceSets.findByName(sourceSetName);
        if (sourceSet == null) {
            throw new org.gradle.api.GradleException(
                "Could not find source set '" + sourceSetName + "' in project '" + 
                projectPath + "' for mod source key: '" + modSourceKey + "'");
        }

        return sourceSet;
    }

    /**
     * Extracts the project path from a mod source key for use as default group ID.
     */
    private static String extractProjectPathFromKey(String modSourceKey) {
        int colonIndex = modSourceKey.lastIndexOf(':');
        if (colonIndex == -1) {
            return modSourceKey;
        }
        return modSourceKey.substring(0, colonIndex);
    }

    /**
     * Returns whether this RunSourceSetsImpl has unresolved lazy mod sources.
     */
    public boolean hasLazyModSources() {
        return !lazyModSources.isEmpty();
    }

    @Override
    public void whenSourceSetAdded(Action<SourceSet> action) {
        this.callbacks.add(action);
        for (SourceSet value : this.sourceSets.values()) {
            action.execute(value);
        }
    }

    /**
     * Holds a lazy mod source reference as string metadata that resolves to an actual SourceSet at execution time.
     */
    private static class LazyModSourceRef {
        final String modSourceKey; // Format: "projectPath:sourceSetName" (e.g., ":api:main")
        final String groupId; // null means use default from project path

        LazyModSourceRef(String modSourceKey, String groupId) {
            this.modSourceKey = modSourceKey;
            this.groupId = groupId;
        }
    }
}
