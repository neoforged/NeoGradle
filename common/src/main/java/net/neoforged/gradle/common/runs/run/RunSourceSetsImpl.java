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
        //Realize all lazy source sets
        if (!this.sourceSetProviders.isEmpty()) {
            final var providers = new ArrayList<>(this.sourceSetProviders);
            this.sourceSetProviders.clear();
            for (Provider<Multimap<String, SourceSet>> sourceSetProvider : providers) {
                final Multimap<String, SourceSet> sourceSets = sourceSetProvider.get();
                sourceSets.forEach(this::add);
            }
        }

        return this.project.provider(() -> this.sourceSets);
    }

    @Override
    public void whenSourceSetAdded(Action<SourceSet> action) {
        this.callbacks.add(action);
        for (SourceSet value : this.sourceSets.values()) {
            action.execute(value);
        }
    }
}
