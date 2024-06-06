package net.neoforged.gradle.common.runs.run;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.util.SourceSetUtils;
import net.neoforged.gradle.dsl.common.runs.run.RunSourceSets;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;

import javax.inject.Inject;

public abstract class RunSourceSetImpl implements RunSourceSets {

    private final Project project;
    private final Provider<Multimap<String, SourceSet>> provider;
    private final Multimap<String, SourceSet> sourceSets;

    @Inject
    public RunSourceSetImpl(Project project) {
        this.project = project;
        this.sourceSets = HashMultimap.create();
        this.provider = project.provider(() -> sourceSets);
    }


    @Override
    public void add(SourceSet sourceSet) {
        this.sourceSets.put(SourceSetUtils.getModIdentifier(sourceSet, null), sourceSet);
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
    }

    @Override
    public void add(String groupId, Iterable<? extends SourceSet> sourceSets) {
        this.sourceSets.putAll(groupId, sourceSets);
    }

    @Override
    public void add(String groupId, SourceSet... sourceSets) {
        for (SourceSet sourceSet : sourceSets) {
            add(groupId, sourceSet);
        }
    }

    @Override
    public Provider<Multimap<String, SourceSet>> all() {
        return this.provider;
    }
}
