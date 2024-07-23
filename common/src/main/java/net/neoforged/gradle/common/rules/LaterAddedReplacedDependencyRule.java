package net.neoforged.gradle.common.rules;

import net.neoforged.gradle.common.util.constants.RunsConstants;
import net.neoforged.gradle.common.util.run.RunsUtil;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Subsystems;
import net.neoforged.gradle.dsl.common.runs.run.Run;
import org.apache.tools.ant.TaskContainer;
import org.gradle.api.NamedDomainObjectCollection;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;

import java.util.HashSet;
import java.util.Set;

public class LaterAddedReplacedDependencyRule implements Rule {

    private final Project project;
    private final NamedDomainObjectContainer<Run> runs;

    @SuppressWarnings("unchecked")
    public LaterAddedReplacedDependencyRule(Project project) {
        this.runs = (NamedDomainObjectContainer<Run>) project.getExtensions().getByName(RunsConstants.Extensions.RUNS);
        this.project = project;
    }

    @Override
    public String getDescription() {
        return "Pattern run<RunName>: Runs the specified run.";
    }

    @Override
    public void apply(String domainObjectName) {
        if (!domainObjectName.startsWith("run")) {
            return;
        }

        final Conventions conventions = project.getExtensions().getByType(Subsystems.class).getConventions();
        if (conventions.getIsEnabled().get() && conventions.getRuns().getIsEnabled().get() && conventions.getRuns().getShouldDefaultRunsBeCreated().get()) {
            final String runName = domainObjectName.substring(3);

            Run run = runs.findByName(runName);
            if (run == null) {
                final String decapitalizedRunName = runName.substring(0, 1).toLowerCase() + runName.substring(1);
                run = runs.findByName(decapitalizedRunName);
                if (run == null) {
                    runs.create(decapitalizedRunName);
                }
            }
        }
    }
}
