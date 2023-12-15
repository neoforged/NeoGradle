package net.neoforged.gradle.platform.runtime.runtime.extension;

import codechicken.diffpatch.util.PatchMode;
import net.neoforged.gradle.common.runtime.extensions.CommonRuntimeExtension;
import net.neoforged.gradle.dsl.common.extensions.Mappings;
import net.neoforged.gradle.dsl.common.extensions.Minecraft;
import net.neoforged.gradle.dsl.common.tasks.ArtifactProvider;
import net.neoforged.gradle.dsl.common.util.Artifact;
import net.neoforged.gradle.dsl.common.util.CommonRuntimeUtils;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.neoform.runtime.definition.NeoFormRuntimeDefinition;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.neoform.util.NeoFormRuntimeUtils;
import net.neoforged.gradle.platform.runtime.runtime.definition.RuntimeDevRuntimeDefinition;
import net.neoforged.gradle.platform.runtime.runtime.specification.RuntimeDevRuntimeSpecification;
import net.neoforged.gradle.platform.runtime.runtime.tasks.ApplyPatches;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;

public abstract class RuntimeDevRuntimeExtension extends CommonRuntimeExtension<RuntimeDevRuntimeSpecification, RuntimeDevRuntimeSpecification.Builder, RuntimeDevRuntimeDefinition> {
    
    @Inject
    public RuntimeDevRuntimeExtension(Project project) {
        super(project);
    }
    
    @Override
    protected @NotNull RuntimeDevRuntimeDefinition doCreate(RuntimeDevRuntimeSpecification spec) {
        final NeoFormRuntimeExtension neoFormRuntimeExtension = getProject().getExtensions().getByType(NeoFormRuntimeExtension.class);
        
        final Artifact neoFormArtifact = spec.getNeoFormArtifact();
        
        final File workingDirectory = spec.getProject().getLayout().getBuildDirectory().dir(String.format("platform/%s", spec.getIdentifier())).get().getAsFile();
        
        final NeoFormRuntimeDefinition joinedNeoFormRuntimeDefinition = neoFormRuntimeExtension.maybeCreate(builder -> {
            builder.withNeoFormArtifact(neoFormArtifact)
                    .withDistributionType(DistributionType.JOINED)
                    .withAdditionalDependencies(spec.getAdditionalDependencies());
            
            NeoFormRuntimeUtils.configureDefaultRuntimeSpecBuilder(spec.getProject(), builder);
        });
        
        final TaskProvider<ApplyPatches> patchApply = spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "applyPatches"), ApplyPatches.class, task -> {
            task.getBase().set(joinedNeoFormRuntimeDefinition.getSourceJarTask().flatMap(ArtifactProvider::getOutput));
            task.getPatches().set(spec.getPatchesDirectory());
            task.getRejects().set(spec.getRejectsDirectory());
            task.getPatchMode().set(spec.isUpdating() ? PatchMode.FUZZY : PatchMode.ACCESS);
            task.getShouldFailOnPatchFailure().set(!spec.isUpdating());
            configureCommonRuntimeTaskParameters(task, "applyPatches", spec, workingDirectory);
        });
        
        final TaskProvider<ArtifactProvider> sourcesProvider = spec.getProject().getTasks().register(CommonRuntimeUtils.buildTaskName(spec, "sourceFromAppliedPatches"), ArtifactProvider.class, task -> {
            task.getInput().set(patchApply.flatMap(ApplyPatches::getOutput));
            task.getOutput().set(new File(workingDirectory, "patched.jar"));
        });
        
        return new RuntimeDevRuntimeDefinition(
                spec,
                joinedNeoFormRuntimeDefinition,
                sourcesProvider
        );
    }
    
    @Override
    protected RuntimeDevRuntimeSpecification.Builder createBuilder() {
        return RuntimeDevRuntimeSpecification.Builder.from(getProject());
    }
    
    @Override
    protected void bakeDefinition(RuntimeDevRuntimeDefinition definition) {
        final RuntimeDevRuntimeSpecification spec = definition.getSpecification();
        final Minecraft minecraftExtension = spec.getProject().getExtensions().getByType(Minecraft.class);
        final Mappings mappingsExtension = minecraftExtension.getMappings();
        
        definition.onBake(
                mappingsExtension.getChannel().get(),
                spec.getProject().getLayout().getBuildDirectory().get().dir("userdev").dir(spec.getIdentifier()).getAsFile()
        );
    }
}
