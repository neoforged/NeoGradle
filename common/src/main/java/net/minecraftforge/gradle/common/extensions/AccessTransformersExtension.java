package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.extensions.base.BaseFilesWithEntriesExtension;
import net.minecraftforge.gradle.dsl.common.extensions.AccessTransformers;
import org.gradle.api.Project;

import javax.inject.Inject;

public abstract class AccessTransformersExtension extends BaseFilesWithEntriesExtension<AccessTransformers> implements AccessTransformers {

    @Inject
    public AccessTransformersExtension(Project project) {
        super(project);
    }
}
