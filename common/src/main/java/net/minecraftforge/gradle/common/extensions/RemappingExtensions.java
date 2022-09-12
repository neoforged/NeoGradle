package net.minecraftforge.gradle.common.extensions;

import net.minecraftforge.gradle.common.config.MCPConfigV2;
import net.minecraftforge.gradle.common.util.MinecraftRepo;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.srgutils.IMappingFile;
import net.minecraftforge.srgutils.IRenamer;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

public abstract class RemappingExtensions {

    private final Project project;

    @Inject
    public RemappingExtensions(Project project) {
        this.project = project;
    }

    private final Provider<ArtifactDownloaderExtension> getDownloader() {
        return project.provider(() -> project.getExtensions().getByType(ArtifactDownloaderExtension.class));
    }

    public Provider<IMappingFile> remapSrgClasses(MCPConfigV2 config, IMappingFile obfToSrg) {
        String minecraftVersion = MinecraftRepo.getMCVersion(config.getVersion());
        return getDownloader().flatMap(d -> d.generate("net.minecraft:client:" + minecraftVersion + ":mappings@txt", true))
                .map(TransformerUtils.guard(
                        client -> {
                            IMappingFile obfToOfficial = IMappingFile.load(client).reverse();

                            return obfToSrg.rename(new IRenamer() {
                                @Override
                                public String rename(IMappingFile.IClass value) {
                                    return obfToOfficial.remapClass(value.getOriginal());
                                }
                            });
                        }));
    }
}
