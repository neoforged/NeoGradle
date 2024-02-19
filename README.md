# NeoGradle
[![Release](https://github.com/neoforged/NeoGradle/actions/workflows/release.yml/badge.svg?branch=NG_7.0)](https://github.com/neoforged/NeoGradle/actions/workflows/release.yml)

---

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

## Run specific dependency management
This implements run specific dependency management for the classpath of a run.
In the past this had to happen via a manual modification of the "minecraft_classpath" token, however tokens don't exist anymore as a component that can be configured on a run.
It was as such not possible to add none FML aware libraries to your classpath of a run.
This PR enables this feature again.


### Usage:
#### Direct
```groovy
dependencies {
    implementation 'some:library:1.2.3'
}

runs {
   testRun {
      dependencies {
         runtime 'some:library:1.2.3'
      }
   }
}
```
#### Configuration
```groovy
configurations {
   libraries {}
   implementation.extendsFrom libraries
}

dependencies {
    libraries 'some:library:1.2.3'
}

runs {
   testRun {
      dependencies {
         runtime project.configurations.libraries
      }
   }
}
```
#### Run Dependency Handler
The dependency handler on a run works very similar to a projects own dependency handler, however it has only one "configuration" available to add dependencies to: "runtime". Additionally, it provides a method to use when you want to turn an entire configuration into a runtime dependency.

## Handling of None-NeoGradle sibling projects
In general, we suggest, no strongly encourage, to **not** use fat jars for this solution.
The process of creating a fat jar with all the code from your sibling projects is difficult to model in a way that is both correct and efficient for a dev project, especially if the sibling project does not use NeoGradle.

### Sibling project uses a NeoGradle module
If it is possible to use a NeoGradle module (for example the Vanilla module, instead of VanillaGradle) then you can use the source-set's mod identifier:
```groovy
sourceSets {
    main {
        run {
            modIdentifier '<some string that all projects in your fat jar have in common>'
        }
    }
}
```
The value of the modIdentifier does not matter here, all projects with the same source-set mod identifier will be included in the same fake fat jar when running your run.

### Sibling project does not use NeoGradle
If the sibling project does not use NeoGradle, then you have to make sure that its Manifest is configured properly:
```text
FMLModType: GAMELIBRARY #Or any other mod type that is not a mod, like LIBRARY
Automatic-Module-Name: '<some string that is unique to this project>'
```
> [!CAUTION]
> If you do this, then your sibling projects are not allowed to contain a class in the same package! This is because no two modules are allowed to contain the same package.
> If you have two sibling projects with a class in the same package, then you will need to move one of them!

### Including the sibling project in your run
To include the sibling project in your run, you need to add it as a modSource to your run:
```groovy
runs {
    someRun {
        modSource sourceSets.main
        modSource project(':siblingProject').sourceSets.main
    }
}
``` 
No other action is needed.
