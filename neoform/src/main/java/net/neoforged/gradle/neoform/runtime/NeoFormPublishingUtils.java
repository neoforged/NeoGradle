package net.neoforged.gradle.neoform.runtime;

import net.neoforged.gradle.dsl.common.util.ConfigurationUtils;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormSdk;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmVersion;
import org.gradle.api.plugins.JavaPluginExtension;

import java.io.File;

public class NeoFormPublishingUtils {

    private static final String NEOFORM_SDK_CAPABILITY = "net.neoforged:neoform-sdk:%s";
    private static final String NEOFORM_DEFAULT_GAV_PATTERN = "net.neoforged:neoform:%s";
    private static final String NEOFORM_DEFAULT_CONTEXT_PATTERN = "neoFormSdk%s";
    private static final String NEOFORM_INCLUDEDBUILD_GAV_PATTERN = "net.neoforged:%s:%s";
    private static final String NEOFORM_INCLUDEDBUILD_CONTEXT_PATTERN = "neoFormIncludedBuild%s";
    private static final String NEOFORM_DEPENDENCIES_CAPABILITY = "net.neoforged:neoform-dependencies";

    public static NeoFormSdk downloadAndParseSdkFile(Project project, String version) {
        return parseSdk(downloadSdkFile(project, version));
    }

    public static File downloadSdkFile(Project project, String version) {
        try {
            //First try it with a published coordinate.
            return attemptDownloadSdkFileWith(project, version, NEOFORM_DEFAULT_CONTEXT_PATTERN.formatted(version), NEOFORM_DEFAULT_GAV_PATTERN.formatted(version));
        } catch (Exception e) {
            //No dependency found.
            try {
                //When neoform is included as a build then the above will fail because the gradle GAV and maven GAV are different
                //Now try it with the included build GAV.
                final String mcVersion = version.substring(0, version.indexOf('-'));
                return attemptDownloadSdkFileWith(project, version, NEOFORM_INCLUDEDBUILD_CONTEXT_PATTERN.formatted(version), NEOFORM_INCLUDEDBUILD_GAV_PATTERN.formatted(mcVersion, version));
            } catch (Exception e2) {
                //Both do not exist, so throw the first exception.
                e.addSuppressed(e2);
                throw e;
            }
        }
    }

    private static File attemptDownloadSdkFileWith(Project project, String version, String context, String neoformGav) {
        final Dependency neoformSdkFormatted = project.getDependencyFactory().create(neoformGav)
                .capabilities(caps -> caps.requireCapability(NEOFORM_SDK_CAPABILITY.formatted(version)));

        final Configuration configuration = ConfigurationUtils.temporaryUnhandledConfiguration(
                project.getConfigurations(),
                context,
                toBuild -> {
                    toBuild.attributes(attr -> {
                        attr.attribute(Usage.USAGE_ATTRIBUTE, project.getObjects().named(Usage.class, "sdk"));
                        attr.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, project.getExtensions().getByType(JavaPluginExtension.class).getToolchain().getLanguageVersion().get().asInt());
                    });
                },
                neoformSdkFormatted
        );

        return configuration.getSingleFile();
    }

    public static NeoFormSdk parseSdk(File sdkFile) {
        return NeoFormSdk.get(sdkFile);
    }

    public static Dependency dependencies(Project project, String version) {
        return project.getDependencyFactory().create(NEOFORM_DEFAULT_GAV_PATTERN.formatted(version))
                .capabilities(caps -> {
                    caps.requireCapability(NEOFORM_DEPENDENCIES_CAPABILITY);
                });
    }
}
