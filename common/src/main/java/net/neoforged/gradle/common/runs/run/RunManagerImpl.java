package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.common.util.DelegatingDomainObjectContainer;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RunManagerImpl extends DelegatingDomainObjectContainer<Run> implements RunManager {

    private final List<Action<Run>> actions = new ArrayList<>();
    private final List<Run> internalRuns = new ArrayList<>();

    private static NamedDomainObjectContainer<Run> createAndRegisterContainer(Project project) {
        final NamedDomainObjectContainer<Run> container = project.container(Run.class, name -> project.getObjects().newInstance(RunImpl.class, project, name));
        project.getExtensions().add("runs", container);
        return container;
    }

    @Inject
    public RunManagerImpl(Project project) {
        super(createAndRegisterContainer(project));
    }

    @Override
    public void addInternal(Run run) {
        internalRuns.add(run);

        for (Action<Run> action : actions) {
            action.execute(run);
        }
    }

    @Override
    public void realizeAll(Action<Run> forAll) {
        super.all(forAll);

        this.actions.add(forAll);

        for (Run run : internalRuns) {
            forAll.execute(run);
        }
    }

    @Override
    public void configureAll(Action<Run> configure) {
        super.configureEach(configure);

        this.actions.add(configure);

        for (Run run : internalRuns) {
            configure.execute(run);
        }
    }
}
