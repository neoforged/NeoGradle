package net.neoforged.gradle.common.runs.run;

import net.neoforged.gradle.common.util.DelegatingDomainObjectContainer;
import net.neoforged.gradle.dsl.common.runs.run.Running;
import net.neoforged.gradle.dsl.common.runs.run.RunManager;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class RunManagerImpl extends DelegatingDomainObjectContainer<Running> implements RunManager {

    private final List<Action<Running>> actions      = new ArrayList<>();
    private final List<Running>         internalRuns = new ArrayList<>();

    private static NamedDomainObjectContainer<Running> createAndRegisterContainer(Project project) {
        final NamedDomainObjectContainer<Running> container = project.container(Running.class, name -> project.getObjects().newInstance(RunImpl.class, project, name));
        project.getExtensions().add("runs", container);
        return container;
    }

    @Inject
    public RunManagerImpl(Project project) {
        super(createAndRegisterContainer(project));
    }

    @Override
    public void addInternal(Running run) {
        internalRuns.add(run);

        for (Action<Running> action : actions) {
            action.execute(run);
        }
    }

    @Override
    public void realizeAll(Action<Running> forAll) {
        super.all(forAll);

        this.actions.add(forAll);

        for (Running run : internalRuns) {
            forAll.execute(run);
        }
    }

    @Override
    public void configureAll(Action<Running> configure) {
        super.configureEach(configure);

        this.actions.add(configure);

        for (Running run : internalRuns) {
            configure.execute(run);
        }
    }
}
