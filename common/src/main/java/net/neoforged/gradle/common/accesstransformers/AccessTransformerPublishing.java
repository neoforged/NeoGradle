package net.neoforged.gradle.common.accesstransformers;

import net.neoforged.gradle.common.extensions.AccessTransformersExtension;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;

public class AccessTransformerPublishing {

    public static final String ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION = "accessTransformerElements";
    public static final String ACCESS_TRANSFORMER_API_CONFIGURATION = "accessTransformerApi";
    public static final String ACCESS_TRANSFORMER_CONFIGURATION = "accessTransformer";
    public static final String ACCESS_TRANSFORMER_CATEGORY = "accesstransformer";

    public static void setup(Project project) {
        AccessTransformers accessTransformersExtension = project.getExtensions().getByType(AccessTransformers.class);

        Configuration accessTransformerElements = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION);
        Configuration accessTransformerApi = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_API_CONFIGURATION);
        Configuration accessTransformer = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_CONFIGURATION);

        accessTransformerApi.setCanBeConsumed(false);
        accessTransformerApi.setCanBeResolved(false);

        accessTransformer.setCanBeConsumed(false);
        accessTransformer.setCanBeResolved(true);

        accessTransformerElements.setCanBeConsumed(true);
        accessTransformerElements.setCanBeResolved(false);
        accessTransformerElements.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, ACCESS_TRANSFORMER_CATEGORY));
        };

        accessTransformerElements.attributes(action);
        accessTransformer.attributes(action);

        accessTransformer.extendsFrom(accessTransformerApi);
        accessTransformerElements.extendsFrom(accessTransformerApi);

        // Now we set up the component, conditionally
        AdhocComponentWithVariants java = (AdhocComponentWithVariants) project.getComponents().getByName("java");
        Runnable enable = () -> java.addVariantsFromConfiguration(accessTransformerElements, variant -> {
        });

        accessTransformerElements.getAllDependencies().configureEach(dep -> enable.run());
        accessTransformerElements.getArtifacts().configureEach(artifact -> enable.run());

        // And add resolved ATs to the extension
        accessTransformersExtension.getFiles().from(accessTransformer);
    }
}
