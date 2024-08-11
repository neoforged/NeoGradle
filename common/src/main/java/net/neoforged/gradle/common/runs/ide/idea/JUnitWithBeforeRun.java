package net.neoforged.gradle.common.runs.ide.idea;

import org.gradle.api.Action;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.PolymorphicDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.tooling.internal.adapter.ObjectGraphAdapter;
import org.jetbrains.gradle.ext.*;

import javax.inject.Inject;
import java.util.Map;

public class JUnitWithBeforeRun extends JUnit {

    private final ExtensiblePolymorphicDomainObjectContainer<BeforeRunTask> beforeRun;

    @Inject
    public JUnitWithBeforeRun(final Project project, final String nameParam) {
        super(nameParam);
        beforeRun = createBeforeRun(project);
    }

    private static ExtensiblePolymorphicDomainObjectContainer<BeforeRunTask> createBeforeRun(Project project) {
        ExtensiblePolymorphicDomainObjectContainer<BeforeRunTask> beforeRun = project.getObjects().polymorphicDomainObjectContainer(BeforeRunTask.class);

        beforeRun.registerFactory(Make.class, makeName -> project.getObjects().newInstance(Make.class, makeName));
        beforeRun.registerFactory(GradleTask.class, gradleTaskName -> project.getObjects().newInstance(GradleTask.class, gradleTaskName));
        beforeRun.registerFactory(BuildArtifact.class, buildArtifactName -> project.getObjects().newInstance(BuildArtifact.class, buildArtifactName));

        return beforeRun;
    }

    public void beforeRun(Action<PolymorphicDomainObjectContainer<BeforeRunTask>> action) {
        action.execute(beforeRun);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Map<String, ?> toMap() {
        final Map map = super.toMap();

        map.put("beforeRun", beforeRun.stream().map(BeforeRunTask::toMap).toList());

        return map;
    }
}
