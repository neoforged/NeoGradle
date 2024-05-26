package net.neoforged.gradle.neoform.runtime.tasks;

import net.neoforged.gradle.common.runtime.tasks.RuntimeArgumentsImpl;
import net.neoforged.gradle.common.runtime.tasks.RuntimeMultiArgumentsImpl;
import net.neoforged.gradle.dsl.common.runtime.tasks.Runtime;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeArguments;
import net.neoforged.gradle.dsl.common.runtime.tasks.RuntimeMultiArguments;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import net.neoforged.gradle.util.ZipBuildingFileTreeVisitor;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.work.DisableCachingByDefault;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

@DisableCachingByDefault(because = "Jar tasks are not cached either")
public abstract class PackJar extends Zip implements Runtime {

   private final Property<JavaLanguageVersion> javaVersion;
   private final Provider<JavaToolchainService> javaToolchainService;
   private final RuntimeArguments arguments;
   private final RuntimeMultiArguments multiArguments;

   public PackJar() {
      super();

      arguments = getObjectFactory().newInstance(RuntimeArgumentsImpl.class, getProviderFactory());
      multiArguments = getObjectFactory().newInstance(RuntimeMultiArgumentsImpl.class, getProviderFactory());

      this.javaVersion = getProject().getObjects().property(JavaLanguageVersion.class);

      final JavaToolchainService service = getProject().getExtensions().getByType(JavaToolchainService.class);
      this.javaToolchainService = getProviderFactory().provider(() -> service);

      this.getArchiveExtension().set("jar");
      this.setPreserveFileTimestamps(false);
      this.setReproducibleFileOrder(true);

      this.from(getInputFiles());

      //Sets up the base configuration for directories and outputs.
      getStepsDirectory().convention(getRuntimeDirectory().dir("steps"));

      //And configure output default locations.
      getOutputDirectory().convention(getStepsDirectory().flatMap(d -> getStepName().map(d::dir)));
      getOutputFileName().convention(getArguments().getOrDefault("outputExtension", getProviderFactory().provider(() -> "jar")).map(extension -> String.format("output.%s", extension)).orElse("output.jar"));
      getOutput().convention(getOutputDirectory().flatMap(d -> getOutputFileName().orElse("output.jar").map(d::file)));

      getOutputDirectory().finalizeValueOnRead();
   }

   @Override
   public Provider<RegularFile> getArchiveFile() {
      return getOutput();
   }

   @InputFiles
   @PathSensitive(PathSensitivity.NONE)
   public abstract ConfigurableFileCollection getInputFiles();

   @Inject
   @Override
   public abstract ObjectFactory getObjectFactory();


   @Override
   @Nested
   public RuntimeArguments getArguments() {
      return arguments;
   }

   @Override
   @Nested
   public RuntimeMultiArguments getMultiArguments() {
      return multiArguments;
   }

   @Internal
   public final Provider<JavaToolchainService> getJavaToolChain() {
      return javaToolchainService;
   }

   @Nested
   @Optional
   @Override
   public Property<JavaLanguageVersion> getJavaVersion() {
      return this.javaVersion;
   }

}
