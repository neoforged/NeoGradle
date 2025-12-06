package net.neoforged.gradle.common.accesstransformers;

import net.neoforged.gradle.common.extensions.AccessTransformersExtension;
import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class AccessTransformerPublishing {

    public static final String ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION = "accessTransformerElements";
    public static final String ACCESS_TRANSFORMER_API_CONFIGURATION = "accessTransformerApi";
    public static final String ACCESS_TRANSFORMER_CONFIGURATION = "accessTransformer";
    public static final String ACCESS_TRANSFORMER_CATEGORY = "accesstransformer";

    @SuppressWarnings("UnstableApiUsage")
    public static void setup(Project project) {
        AccessTransformers extension = project.getExtensions().getByType(AccessTransformers.class);

        Configuration elementsConfig = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_ELEMENTS_CONFIGURATION);
        Configuration apiConfig = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_API_CONFIGURATION);
        Configuration implConfig = project.getConfigurations().maybeCreate(ACCESS_TRANSFORMER_CONFIGURATION);

        apiConfig.setCanBeConsumed(false);
        apiConfig.setCanBeResolved(false);

        implConfig.setCanBeConsumed(false);
        implConfig.setCanBeResolved(true);

        elementsConfig.setCanBeConsumed(true);
        elementsConfig.setCanBeResolved(false);
        elementsConfig.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, ACCESS_TRANSFORMER_CATEGORY));
        };

        elementsConfig.attributes(action);
        implConfig.attributes(action);

        implConfig.extendsFrom(apiConfig);
        elementsConfig.extendsFrom(apiConfig);

        // Now we set up the component, conditionally
        AdhocComponentWithVariants java = (AdhocComponentWithVariants) project.getComponents().getByName("java");
        Runnable enable = () -> java.addVariantsFromConfiguration(elementsConfig, variant -> {
        });

        elementsConfig.getAllDependencies().configureEach(dep -> enable.run());
        elementsConfig.getArtifacts().configureEach(artifact -> enable.run());

        // And add resolved ATs to the extension
        extension.getFiles().from(implConfig);

        //When the user has configured the dependency collectors add the relevant files.
        ProjectUtils.afterEvaluate(project, () -> {
            apiConfig.fromDependencyCollector(extension.getConsumeApi());
            implConfig.fromDependencyCollector(extension.getConsume());

            final List<File> files = extension.getFiles().getFiles().stream()
                .sorted(Comparator.comparing(File::getName))
                .toList();
            for (int i = 0; i < files.size(); i++)
            {
                final var file = files.get(i);
                if (files.size() == 1) {
                    extension.expose(file, artifact -> artifact.setClassifier("access-transformer"));
                } else {
                    final int index = i;
                    extension.expose(file, artifact -> artifact.setClassifier("access-transformer-%d".formatted(index)));
                }
            }
        });
    }
}
