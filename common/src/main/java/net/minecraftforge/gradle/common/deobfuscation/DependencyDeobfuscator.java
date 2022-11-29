package net.minecraftforge.gradle.common.deobfuscation;

import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementContext;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementExtension;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementHandler;
import net.minecraftforge.gradle.common.extensions.dependency.replacement.DependencyReplacementResult;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public final class DependencyDeobfuscator {
    private static final DependencyDeobfuscator INSTANCE = new DependencyDeobfuscator();

    public static DependencyDeobfuscator getInstance() {
        return INSTANCE;
    }

    private DependencyDeobfuscator() {
    }

    public void apply(final Project project) {
        final DependencyReplacementExtension dependencyReplacer = project.getExtensions().create("deobfuscation", DependencyReplacementExtension.class, project);
        dependencyReplacer.getReplacementHandlers().add(new DependencyReplacementHandler() {

            @Override
            public @NotNull Optional<DependencyReplacementResult> get(@NotNull DependencyReplacementContext context) {
                if (!(context.dependency() instanceof ExternalModuleDependency)) {
                    return Optional.empty();
                }

                final Configuration resolver = context.project().getConfigurations().detachedConfiguration(context.dependency());
                if (resolver.getResolvedConfiguration().getLenientConfiguration().getFiles().isEmpty()) {
                    return Optional.empty();
                }

                final File file = resolver.getResolvedConfiguration().getLenientConfiguration().getFiles().iterator().next();
                try(final JarInputStream jarStream = new JarInputStream(Files.newInputStream(file.toPath()))) {
                    Manifest mf = jarStream.getManifest();
                    final boolean isObfuscated = mf.getMainAttributes().containsKey("Obfuscated") && Boolean.parseBoolean(mf.getMainAttributes().getValue("Obfuscated"));
                    final boolean obfuscatedByForgeGradle = mf.getMainAttributes().containsKey("Obfuscated-By") && mf.getMainAttributes().getValue("Obfuscated-By").equals("ForgeGradle");
                    if (isObfuscated && obfuscatedByForgeGradle) {
                        return Optional.of(new DependencyReplacementResult(file, context.dependency()));
                    }
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
        });

    }
}
