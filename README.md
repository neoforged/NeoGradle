NeoGradle
===========

Minecraft mod development framework used by NeoForge and FML for the Gradle build system.

For a quick start, see how the [NeoForge Mod Development Kit](https://github.com/neoforged/MDK) uses NeoGradle, or see
our official [Documentation](https://docs.neoforged.net/neogradle/docs/).

To see the latest available version of NeoGradle, visit the [NeoForged project page](https://projects.neoforged.net/neoforged/neogradle).

## Override Decompiler Settings

The settings used by the decompiler when preparing Minecraft dependencies can be overridden
using [Gradle properties](https://docs.gradle.org/current/userguide/project_properties.html).
This can be useful to trade slower build-times for being able to run NeoGradle on lower-end machines.

| Property                                     | Description                                                                                                                |
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| `neogradle.subsystems.decompiler.maxMemory`  | How much heap memory is given to the decompiler. Can be specified either in gigabyte (`4g`) or megabyte (`4096m`).         |
| `neogradle.subsystems.decompiler.maxThreads` | By default the decompiler uses all available CPU cores. This setting can be used to limit it to a given number of threads. |
| `neogradle.subsystems.decompiler.logLevel`   | Can be used to override the [decompiler loglevel](https://vineflower.org/usage/#cmdoption-log).                            |

## Override Recompiler Settings

The settings used by Neogradle for recompiling the decompiled Minecraft source code can be customized
using [Gradle properties](https://docs.gradle.org/current/userguide/project_properties.html).

| Property                                    | Description                                                                                                                          |
|---------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| `neogradle.subsystems.recompiler.maxMemory` | How much heap memory is given to the decompiler. Can be specified either in gigabyte (`4g`) or megabyte (`4096m`). Defaults to `1g`. |
| `neogradle.subsystems.recompiler.jvmArgs`   | Pass arbitrary JVM arguments to the forked Gradle process that runs the compiler. I.e. `-XX:+HeapDumpOnOutOfMemoryError`             |
| `neogradle.subsystems.recompiler.args`      | Pass additional command line arguments to the Java compiler.                                                                         |
