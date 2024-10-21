package net.neoforged.gradle.common.interfaceinjection;

import net.neoforged.gradle.dsl.common.extensions.InterfaceInjections;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;

public class InterfaceInjectionPublishing {

    public static final String INTERFACE_INJECTION_ELEMENTS_CONFIGURATION = "InterfaceInjectionElements";
    public static final String INTERFACE_INJECTION_API_CONFIGURATION = "InterfaceInjectionApi";
    public static final String INTERFACE_INJECTION_CONFIGURATION = "InterfaceInjection";
    public static final String INTERFACE_INJECTION_CATEGORY = "interfaceInjection";

    public static void setup(Project project) {
        InterfaceInjections InterfaceInjectionsExtension = project.getExtensions().getByType(InterfaceInjections.class);

        Configuration InterfaceInjectionElements = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_ELEMENTS_CONFIGURATION);
        Configuration InterfaceInjectionApi = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_API_CONFIGURATION);
        Configuration InterfaceInjection = project.getConfigurations().maybeCreate(INTERFACE_INJECTION_CONFIGURATION);

        InterfaceInjectionApi.setCanBeConsumed(false);
        InterfaceInjectionApi.setCanBeResolved(false);

        InterfaceInjection.setCanBeConsumed(false);
        InterfaceInjection.setCanBeResolved(true);

        InterfaceInjectionElements.setCanBeConsumed(true);
        InterfaceInjectionElements.setCanBeResolved(false);
        InterfaceInjectionElements.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, INTERFACE_INJECTION_CATEGORY));
        };

        InterfaceInjectionElements.attributes(action);
        InterfaceInjection.attributes(action);

        InterfaceInjection.extendsFrom(InterfaceInjectionApi);
        InterfaceInjectionElements.extendsFrom(InterfaceInjectionApi);

        // Now we set up the component, conditionally
        AdhocComponentWithVariants java = (AdhocComponentWithVariants) project.getComponents().getByName("java");
        Runnable enable = () -> java.addVariantsFromConfiguration(InterfaceInjectionElements, variant -> {
        });

        InterfaceInjectionElements.getAllDependencies().configureEach(dep -> enable.run());
        InterfaceInjectionElements.getArtifacts().configureEach(artifact -> enable.run());

        // And add resolved ATs to the extension
        InterfaceInjectionsExtension.getFiles().from(InterfaceInjection);
    }
}
