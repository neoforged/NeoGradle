package net.neoforged.gradle.common.extensions.subsystems;

import net.minecraftforge.gdi.BaseDSLElement;
import net.neoforged.gradle.common.extensions.base.WithEnabledProperty;
import net.neoforged.gradle.dsl.common.extensions.subsystems.Conventions;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Configurations;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.IDE;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.Runs;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.SourceSets;
import net.neoforged.gradle.dsl.common.extensions.subsystems.conventions.ide.IDEA;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class ConventionsExtension extends WithEnabledProperty implements BaseDSLElement<Conventions>, Conventions {

    private final Configurations configurations;
    private final SourceSets sourceSets;
    private final IDE ide;
    private final Runs runs;

    @Inject
    public ConventionsExtension(Project project) {
        super(project, "conventions");

        this.configurations = project.getObjects().newInstance(ConfigurationsExtension.class, this);
        this.sourceSets = project.getObjects().newInstance(SourceSetsExtension.class, this);
        this.ide = project.getObjects().newInstance(IDEExtension.class, this);
        this.runs = project.getObjects().newInstance(RunsExtension.class, this);
    }

    @Override
    public Configurations getConfigurations() {
        return configurations;
    }

    @Override
    public SourceSets getSourceSets() {
        return sourceSets;
    }

    @Override
    public IDE getIde() {
        return ide;
    }

    @Override
    public Runs getRuns() {
        return runs;
    }

    public static abstract class ConfigurationsExtension extends WithEnabledProperty implements BaseDSLElement<Configurations>, Configurations {

        @Inject
        public ConfigurationsExtension(WithEnabledProperty parent) {
            super(parent, "configurations");

            getLocalRuntimeConfigurationPostFix().convention(getStringProperty("localRuntimeConfigurationPostFix").orElse("LocalRuntime"));
            getRunRuntimeConfigurationPostFix().convention(getStringProperty("perSourceSetRunRuntimeConfigurationPostFix").orElse("LocalRunRuntime"));
            getPerRunRuntimeConfigurationPostFix().convention(getStringProperty("perRunRuntimeConfigurationPostFix").orElse("Run"));
            getRunRuntimeConfigurationName().convention(getStringProperty("runRuntimeConfigurationName").orElse("runs"));
        }
    }

    public static abstract class SourceSetsExtension extends WithEnabledProperty implements BaseDSLElement<SourceSets>, SourceSets {

        @Inject
        public SourceSetsExtension(WithEnabledProperty parent) {
            super(parent, "sourcesets");

            getShouldMainSourceSetBeAutomaticallyAddedToRuns().convention(getBooleanProperty("automatic-inclusion").orElse(true));
            getShouldSourceSetsLocalRunRuntimesBeAutomaticallyAddedToRuns().convention(getBooleanProperty("automatic-inclusion-local-run-runtime").orElse(true));
        }
    }

    public static abstract class IDEExtension extends WithEnabledProperty implements BaseDSLElement<IDE>, IDE {

        private final IDEA idea;

        @Inject
        public IDEExtension(WithEnabledProperty parent) {
            super(parent, "ide");

            this.idea = getProject().getObjects().newInstance(IDEAExtension.class, this);
        }

        @Override
        public IDEA getIdea() {
            return idea;
        }
    }

    public static abstract class RunsExtension extends WithEnabledProperty implements BaseDSLElement<Runs>, Runs {

        @Inject
        public RunsExtension(WithEnabledProperty parent) {
            super(parent, "runs");

            getShouldDefaultRunsBeCreated().convention(getBooleanProperty("create-default-run-per-type").orElse(true));
            getShouldDefaultTestTaskBeReused().convention(getBooleanProperty("reuse-default-test-task").orElse(true));
        }
    }

    public static abstract class IDEAExtension extends WithEnabledProperty implements BaseDSLElement<IDEA>, IDEA {

        @Inject
        public IDEAExtension(WithEnabledProperty parent) {
            super(parent, "idea");

            getShouldUseCompilerDetection().convention(getBooleanProperty("compiler-detection").orElse(true));
            getShouldUsePostSyncTask().convention(getBooleanProperty("use-post-sync-task").orElse(false));
        }
    }
}
