# NeoGradle

Minecraft mod development framework, used by NeoForge and FML for the Gradle build system.

For a quick start, see how the [NeoForge Mod Development Kit](https://github.com/neoforged/MDK) uses NeoGradle, or see
our official [Documentation](https://docs.neoforged.net/neogradle/docs/).

To see the latest available version of NeoGradle, visit the [NeoForged project page](https://projects.neoforged.net/neoforged/neogradle).

## Plugins

NeoGradle is separated into several different plugins that can be applied independently of each other.

### Userdev Plugin

This plugin is used for building mods with NeoForge. As a modder, this will in many cases be the only plugin you use.

```gradle
plugins {
  id 'net.neoforged.gradle.userdev' version '<neogradle_version>'
}

dependencies {
  implementation 'net.neoforged:neoforge:<neoforge_version>'
}
```

When this plugin detects a dependency on NeoForge, it will spring into action and create the necessary NeoForm runtime tasks to build a usable Minecraft JAR-file that contains the requested NeoForge version.

### NeoForm Runtime Plugin

This plugin enables use of the NeoForm runtime and allows projects to depend directly on deobfuscated but otherwise
unmodified Minecraft artifacts.

This plugin is used internally by other plugins and is usually only needed for advanced use cases.

```gradle
plugins {
  id 'net.neoforged.gradle.neoform' version '<neogradle_version>'
}

dependencies {
  // For depending on a Minecraft JAR-file with both client- and server-classes
  implementation "net.minecraft:neoform_joined:<neoform-version>'
  
  // For depending on the Minecraft client JAR-file
  implementation "net.minecraft:neoform_client:<neoform-version>'
  
  // For depending on the Minecraft dedicated server JAR-file
  implementation "net.minecraft:neoform_server:<neoform-version>'
}
```

## Apply Parchment Mappings

To get human-readable parameter names in decompiled Minecraft source-code, as well as Javadocs, crowdsourced data
from the [Parchment project](https://parchmentmc.org) can be applied to the Minecraft source-code before it is recompiled.

This is currently only supported when applying the NeoGradle userdev Plugin.

The most basic configuration is using the following properties in gradle.properties:

```
neogradle.subsystems.parchment.minecraftVersion=1.20.2
neogradle.subsystems.parchment.mappingsVersion=2023.12.10
```

The subsystem also has a Gradle DSL and supports more parameters, explained in the following Gradle snippet:

```gradle
subsystems {
  parchment {
    // The Minecraft version for which the Parchment mappings were created.
    // This does not necessarily need to match the Minecraft version your mod targets
    // Defaults to the value of Gradle property neogradle.subsystems.parchment.minecraftVersion
    minecraftVersion = "1.20.2"
    
    // The version of Parchment mappings to apply.
    // See https://parchmentmc.org/docs/getting-started for a list.
    // Defaults to the value of Gradle property neogradle.subsystems.parchment.mappingsVersion
    mappingsVersion = "2023.12.10"
    
    // Overrides the full Maven coordinate of the Parchment artifact to use
    // This is computed from the minecraftVersion and mappingsVersion properties by default.
    // If you set this property explicitly, minecraftVersion and mappingsVersion will be ignored.
    // The built-in default value can also be overriden using the Gradle property neogradle.subsystems.parchment.parchmentArtifact
    // parchmentArtifact = "org.parchmentmc.data:parchment-$minecraftVersion:$mappingsVersion:checked@zip"
    
    // Overrides the full Maven coordinate of the tool used to apply the Parchment mappings
    // See https://github.com/neoforged/JavaSourceTransformer
    // The built-in default value can also be overriden using the Gradle property neogradle.subsystems.parchment.toolArtifact
    // toolArtifact = "net.neoforged.jst:jst-cli-bundle:1.0.30"
    
    // Set this to false if you don't want the https://maven.parchmentmc.org/ repository to be added automatically when
    // applying Parchment mappings is enabled
    // The built-in default value can also be overriden using the Gradle property neogradle.subsystems.parchment.addRepository
    // addRepository = true
    
    // Can be used to explicitly disable this subsystem. By default, it will be enabled automatically as soon
    // as parchmentArtifact or minecraftVersion and mappingsVersion are set.
    // The built-in default value can also be overriden using the Gradle property neogradle.subsystems.parchment.enabled
    // enabled = true
  }
}
```

## Advanced Settings

### Override Decompiler Settings

The settings used by the decompiler when preparing Minecraft dependencies can be overridden
using [Gradle properties](https://docs.gradle.org/current/userguide/project_properties.html).
This can be useful to run NeoGradle on lower-end machines, at the cost of slower build times.

| Property                                     | Description                                                                                                                |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `neogradle.subsystems.decompiler.maxMemory`  | How much heap memory is given to the decompiler. Can be specified either in gigabyte (`4g`) or megabyte (`4096m`).         |
| `neogradle.subsystems.decompiler.maxThreads` | By default the decompiler uses all available CPU cores. This setting can be used to limit it to a given number of threads. |
| `neogradle.subsystems.decompiler.logLevel`   | Can be used to override the [decompiler loglevel](https://vineflower.org/usage/#cmdoption-log).                            |

### Override Recompiler Settings

The settings used by Neogradle for recompiling the decompiled Minecraft source code can be customized
using [Gradle properties](https://docs.gradle.org/current/userguide/project_properties.html).

| Property                                    | Description                                                                                                                          |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `neogradle.subsystems.recompiler.maxMemory` | How much heap memory is given to the decompiler. Can be specified either in gigabyte (`4g`) or megabyte (`4096m`). Defaults to `1g`. |
| `neogradle.subsystems.recompiler.jvmArgs`   | Pass arbitrary JVM arguments to the forked Gradle process that runs the compiler. I.e. `-XX:+HeapDumpOnOutOfMemoryError`             |
| `neogradle.subsystems.recompiler.args`      | Pass additional command line arguments to the Java compiler.                                                                         |
