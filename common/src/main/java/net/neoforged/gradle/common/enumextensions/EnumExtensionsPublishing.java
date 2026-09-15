package net.neoforged.gradle.common.enumextensions;

import net.neoforged.gradle.common.util.ProjectUtils;
import net.neoforged.gradle.dsl.common.extensions.EnumExtensions;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;

import java.io.File;
import java.util.Comparator;
import java.util.List;

public class EnumExtensionsPublishing {

    public static final String ENUM_EXTENSIONS_ELEMENTS_CONFIGURATION = "enumExtensionsElements";
    public static final String ENUM_EXTENSIONS_API_CONFIGURATION = "enumExtensionsApi";
    public static final String ENUM_EXTENSIONS_CONFIGURATION = "enumExtensions";
    public static final String ENUM_EXTENSIONS_CATEGORY = "enumExtensions";

    @SuppressWarnings("UnstableApiUsage")
    public static void setup(Project project) {
        EnumExtensions extension = project.getExtensions().getByType(EnumExtensions.class);

        Configuration elementsConfig = project.getConfigurations().maybeCreate(ENUM_EXTENSIONS_ELEMENTS_CONFIGURATION);
        Configuration apiConfig = project.getConfigurations().maybeCreate(ENUM_EXTENSIONS_API_CONFIGURATION);
        Configuration implementationConfig = project.getConfigurations().maybeCreate(ENUM_EXTENSIONS_CONFIGURATION);

        apiConfig.setCanBeConsumed(false);
        apiConfig.setCanBeResolved(false);

        implementationConfig.setCanBeConsumed(false);
        implementationConfig.setCanBeResolved(true);

        elementsConfig.setCanBeConsumed(true);
        elementsConfig.setCanBeResolved(false);
        elementsConfig.setCanBeDeclared(false);

        Action<AttributeContainer> action = attributes -> {
            attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.getObjects().named(Category.class, ENUM_EXTENSIONS_CATEGORY));
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
                    extension.expose(file, artifact -> artifact.setClassifier("enum-extensions"));
                } else {
                    final int index = i;
                    extension.expose(file, artifact -> artifact.setClassifier("enum-extensions-%d".formatted(index)));
                }
            }
        });
    }
}
