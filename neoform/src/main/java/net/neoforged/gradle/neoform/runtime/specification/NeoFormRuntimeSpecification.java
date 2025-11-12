package net.neoforged.gradle.neoform.runtime.specification;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.neoforged.gradle.common.runtime.specification.CommonRuntimeSpecification;
import net.neoforged.gradle.common.util.ToolUtilities;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskCustomizer;
import net.neoforged.gradle.dsl.common.runtime.tasks.tree.TaskTreeAdapter;
import net.neoforged.gradle.dsl.common.util.DistributionType;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV2;
import net.neoforged.gradle.dsl.neoform.runtime.specification.NeoFormSpecification;
import net.neoforged.gradle.neoform.runtime.extensions.NeoFormRuntimeExtension;
import net.neoforged.gradle.util.FileUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Defines a specification for an MCP runtime.
 */
public class NeoFormRuntimeSpecification extends CommonRuntimeSpecification implements NeoFormSpecification
{
    private final Provider<Directory>                                                                                             unpackedNeoFormArchive;
    private final NeoFormConfigConfigurationSpecV2                                                                                config;
    private final FileCollection                                                                                                  additionalRecompileDependencies;
    private final Map<String, String>                                                                                             skipTasks;
    private final BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> mutator;
    private final Provider<File>                                                                                                  neoFormArchive;

    private NeoFormRuntimeSpecification(
        final Project project,
        final String version,
        final Provider<Directory> unpackedNeoFormArchive,
        final Provider<File> archive,
        final NeoFormConfigConfigurationSpecV2 config,
        final DistributionType side,
        final Multimap<String, TaskTreeAdapter> preTaskTypeAdapters,
        final Multimap<String, TaskTreeAdapter> postTypeAdapters,
        final Multimap<String, TaskCustomizer<? extends Task>> taskCustomizers,
        final FileCollection additionalRecompileDependencies,
        final Map<String, String> skipTasks,
        final BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> mutator)
    {
        super(project, "neoForm", version, side, preTaskTypeAdapters, postTypeAdapters, taskCustomizers, NeoFormRuntimeExtension.class);
        this.unpackedNeoFormArchive = unpackedNeoFormArchive;
        this.config = config;
        this.additionalRecompileDependencies = additionalRecompileDependencies;
        this.skipTasks = skipTasks;
        this.mutator = mutator;
        this.neoFormArchive = archive;
    }

    public NeoFormConfigConfigurationSpecV2 getConfig()
    {
        return config;
    }

    public String getMinecraftVersion()
    {
        return config.getVersion();
    }

    public String getNeoFormVersion()
    {
        String prefix = getMinecraftVersion() + "-";
        if (getVersion().startsWith(prefix))
        {
            return getVersion().substring(prefix.length());
        }
        else
        {
            throw new RuntimeException("NeoForm version " + getVersion() + " does not start with Minecraft version" + getMinecraftVersion());
        }
    }

    public Provider<Directory> getUnpackedNeoFormArchive()
    {
        return unpackedNeoFormArchive;
    }

    public Provider<File> getNeoFormArchive() {
        return neoFormArchive;
    }

    @Override
    public @NotNull FileCollection getAdditionalRecompileDependencies()
    {
        return additionalRecompileDependencies;
    }

    @Override
    public @Nullable String skipTaskWith(final String taskName)
    {
        return skipTasks.get(taskName);
    }

    @Override
    public BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> getMutator()
    {
        return mutator;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof final NeoFormRuntimeSpecification spec))
        {
            return false;
        }
        if (!super.equals(o))
        {
            return false;
        }

        if (!unpackedNeoFormArchive.equals(spec.unpackedNeoFormArchive))
        {
            return false;
        }
        return additionalRecompileDependencies.equals(spec.additionalRecompileDependencies);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + unpackedNeoFormArchive.hashCode();
        result = 31 * result + additionalRecompileDependencies.hashCode();
        return result;
    }

    public static final class Builder extends CommonRuntimeSpecification.Builder<NeoFormRuntimeSpecification, Builder>
        implements NeoFormSpecification.Builder<NeoFormRuntimeSpecification, Builder>
    {
        private       Dependency                                                                                                      neoFormDependency;
        private       FileCollection                                                                                                  additionalDependencies;
        private final Map<String, String>                                                                                             skipped = Maps.newHashMap();
        private       BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> mutator = (steps, function) -> {};

        private Builder(Project project)
        {
            super(project);
            this.additionalDependencies = project.getObjects().fileCollection();
            withNeoFormVersion("+");
        }

        @Override
        protected Builder getThis()
        {
            return this;
        }

        public static Builder from(final Project project)
        {
            return new Builder(project);
        }

        @NotNull
        @Override
        public Builder withNeoFormVersion(@NotNull String version)
        {
            this.neoFormDependency = project.getDependencies().create("net.neoforged:neoform:" + version + "@zip");
            return getThis();
        }

        @NotNull
        @Override
        public Builder withNeoFormDependency(@NotNull Object notation)
        {
            this.neoFormDependency = getProject().getDependencies().create(notation);
            return getThis();
        }

        @Override
        public Builder withAdditionalDependencies(final FileCollection files)
        {
            this.additionalDependencies = this.additionalDependencies.plus(files);
            return getThis();
        }

        @Override
        public @NotNull Builder withSkippedTask(@NotNull final String task, @NotNull final String outputSource)
        {
            this.skipped.put(task, outputSource);
            return getThis();
        }

        @Override
        public Builder withStepsMutator(@NotNull final BiConsumer<List<NeoFormConfigConfigurationSpecV1.Step>, Map<String, NeoFormConfigConfigurationSpecV1.Function>> mutator)
        {
            this.mutator =
                this.mutator.andThen(mutator);
            return this;
        }

        public @NotNull NeoFormRuntimeSpecification build()
        {
            ResolvedArtifact artifact = ToolUtilities.resolveToolArtifact(project, neoFormDependency);
            File archive = artifact.getFile();
            String effectiveVersion = artifact.getModuleVersion().getId().getVersion();

            // Read the NF config from the archive
            NeoFormConfigConfigurationSpecV2 config;
            try
            {
                config = FileUtils.processFileFromZip(archive, "config.json", NeoFormConfigConfigurationSpecV2::get);
            }
            catch (IOException e)
            {
                throw new GradleException("Failed to read NeoForm config file from version " + effectiveVersion);
            }

            return new NeoFormRuntimeSpecification(
                project,
                effectiveVersion,
                project.getProviders().of(NeoFormUnpack.class, spec -> {
                    spec.parameters(p -> {
                        p.getArchive().set(archive);
                        p.getDestination().set(project.getLayout().getBuildDirectory().dir("neoform/" + effectiveVersion));
                    });
                }),
                project.provider(() -> archive),
                config,
                distributionType.get(),
                preTaskAdapters,
                postTaskAdapters,
                taskCustomizers,
                additionalDependencies,
                skipped,
                mutator);
        }
    }

    public static interface NeoFormUnpackParameters extends ValueSourceParameters
    {

        RegularFileProperty getArchive();

        DirectoryProperty getDestination();
    }

    public static abstract class NeoFormUnpack implements ValueSource<Directory, NeoFormUnpackParameters>
    {

        @Nullable
        @Override
        public Directory obtain()
        {
            RegularFileProperty archive = getParameters().getArchive();
            DirectoryProperty destination = getParameters().getDestination();

            File dest = destination.getAsFile().get();
            if (!dest.exists() && !dest.mkdirs())
            {
                throw new GradleException("Failed to create directory " + dest);
            }

            try (java.util.zip.ZipFile zipFile = new ZipFile(archive.getAsFile().get()))
            {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements())
                {
                    ZipEntry entry = entries.nextElement();
                    File entryDestination = new File(dest, entry.getName());
                    if (entry.isDirectory())
                    {
                        entryDestination.mkdirs();
                    }
                    else
                    {
                        entryDestination.getParentFile().mkdirs();
                        try (InputStream in = zipFile.getInputStream(entry);
                             OutputStream out = new FileOutputStream(entryDestination))
                        {
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            return destination.get();
        }
    }
}
