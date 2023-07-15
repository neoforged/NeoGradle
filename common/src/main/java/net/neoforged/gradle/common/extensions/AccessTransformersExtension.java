package net.neoforged.gradle.common.extensions;

import net.neoforged.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.neoforged.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class AccessTransformersExtension extends BaseFilesWithEntriesExtension<AccessTransformers> implements AccessTransformers {

    @Inject
    public AccessTransformersExtension(Project project) {
        super(project);
    }
}
