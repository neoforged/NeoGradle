package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.neoforged.gradle.common.interfaceinjection.InterfaceInjectionPublishing;
import net.neoforged.gradle.dsl.common.extensions.InjectedInterfaceData;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurablePublishArtifact;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public abstract class InterfaceInjectionsExtension extends BaseFilesWithEntriesExtension<InterfaceInjections, InjectedInterfaceData> implements InterfaceInjections {
    private transient final DependencyHandler projectDependencies;
    private transient final ArtifactHandler projectArtifacts;

    @SuppressWarnings("UnstableApiUsage")
    @Inject
    public InterfaceInjectionsExtension(Project project) {
        super(project);

        this.projectDependencies = project.getDependencies();
        this.projectArtifacts = project.getArtifacts();

        // We have to add these after project evaluation because of dependency replacement making configurations non-lazy; adding them earlier would prevent further addition of dependencies
        project.afterEvaluate(p -> {
            p.getConfigurations().maybeCreate(InterfaceInjectionPublishing.INTERFACE_INJECTION_CONFIGURATION).fromDependencyCollector(getConsume());
            p.getConfigurations().maybeCreate(InterfaceInjectionPublishing.INTERFACE_INJECTION_API_CONFIGURATION).fromDependencyCollector(getConsumeApi());
        });
    }

    @Override
    public void expose(Object path, Action<ConfigurablePublishArtifact> action) {
        file(path);
        projectArtifacts.add(InterfaceInjectionPublishing.INTERFACE_INJECTION_ELEMENTS_CONFIGURATION, path, action);
    }

    @Override
    public void expose(Object path) {
        expose(path, artifacts -> {
        });
    }

    @Override
    public void expose(Dependency dependency) {
        projectDependencies.add(InterfaceInjectionPublishing.INTERFACE_INJECTION_API_CONFIGURATION, dependency);
    }

    @Override
    public void inject(String type, String interfaceName) {
        inject(type, List.of(interfaceName));
    }

    @Override
    public void inject(String type, String... interfaceNames) {
        inject(type, List.of(interfaceNames));
    }

    @Override
    public void inject(String type, Collection<String> interfaceNames) {
        final InjectedInterfaceData data = getProject().getObjects().newInstance(InjectedInterfaceData.class);

        data.getTarget().set(type);
        data.getInterfaces().addAll(interfaceNames);

        entry(data);
    }
}
