package net.neoforged.gradle.common.interfaceinjection;

import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class InterfaceInjectionPublishing {

    public static final String INTERFACE_INJECTION_ELEMENTS_CONFIGURATION = "interfaceInjectionElements";
    public static final String INTERFACE_INJECTION_API_CONFIGURATION = "interfaceInjectionApi";
    public static final String INTERFACE_INJECTION_CONFIGURATION = "interfaceInjection";
    public static final String INTERFACE_INJECTION_CATEGORY = "interfaceinjection";

    @SuppressWarnings("UnstableApiUsage")
    public static void setup(Project project) {
        InterfaceInjections extension = project.getExtensions().getByType(InterfaceInjections.class);

        Configuration elementsConfig = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_ELEMENTS_CONFIGURATION);
        Configuration apiConfig = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_API_CONFIGURATION);
        Configuration implementationConfig = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_CONFIGURATION);

        apiConfig.setCanBeConsumed(false);
        apiConfig.setCanBeResolved(false);

        implementationConfig.setCanBeConsumed(false);
        implementationConfig.setCanBeResolved(true);

        elementsConfig.setCanBeConsumed(true);
        elementsConfig.setCanBeResolved(false);
        elementsConfig.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, INTERFACE_INJECTION_CATEGORY));
        };

        elementsConfig.attributes(action);
        implementationConfig.attributes(action);

        implementationConfig.extendsFrom(apiConfig);
        elementsConfig.extendsFrom(apiConfig);

        // Now we set up the component, conditionally
        AdhocComponentWithVariants java = (AdhocComponentWithVariants) project.getComponents().getByName("java");
        Runnable enable = () -> java.addVariantsFromConfiguration(elementsConfig, variant -> {
        });

        elementsConfig.getAllDependencies().configureEach(dep -> {
            enable.run();
        });
        elementsConfig.getArtifacts().configureEach(artifact -> enable.run());

        // And add resolved iis to the extension
        extension.getFiles().from(implementationConfig);

        //When the user has configured the dependency collectors add the relevant files.
        ProjectUtils.afterEvaluate(project, () -> {
            apiConfig.fromDependencyCollector(extension.getConsumeApi());
            implementationConfig.fromDependencyCollector(extension.getConsume());

            final List<File> files = extension.getFiles().getFiles().stream()
                .sorted(Comparator.comparing(File::getName))
                .toList();
            for (int i = 0; i < files.size(); i++)
            {
                final var file = files.get(i);
                if (files.size() == 1) {
                    extension.expose(file, artifact -> artifact.setClassifier("interface-injections"));
                } else {
                    final int index = i;
                    extension.expose(file, artifact -> artifact.setClassifier("interface-injection-%d".formatted(index)));
                }
            }
        });
    }
}
