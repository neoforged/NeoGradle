package net.neoforged.gradle.platform.model;

import net.minecraftforge.gdi.BaseDSLElement;
import org.gradle.api.provider.Property;

public abstract class GameType implements BaseDSLElement<GameType> {

    /*

    [
                id: id,
                time: timestamp,
                releaseTime: timestamp,
                type: 'release',
                mainClass: 'cpw.mods.bootstraplauncher.BootstrapLauncher',
                inheritsFrom: MC_VERSION,
                logging: {},
                arguments: [
                    game: ['--launchTarget', 'fmlclient',
                           '--fml.forgeVersion', FORGE_VERSION,
                           '--fml.fmlVersion', FANCY_MOD_LOADER_VERSION,
                           '--fml.mcVersion', MC_VERSION,
                           '--fml.mcpVersion', MCP_VERSION],
                    jvm: ['-Djava.net.preferIPv6Addresses=system',
                          "-DignoreList=${foxlauncher_client.properties.ignoreList},\${version_name}.jar",
                          "-DmergeModules=${foxlauncher_client.properties.mergeModules}",
                          "-Dfml.pluginLayerLibraries=${foxlauncher_client.properties['fml.pluginLayerLibraries']}",
                          "-Dfml.gameLayerLibraries=${foxlauncher_client.properties['fml.gameLayerLibraries']}",
                          '-DlibraryDirectory=${library_directory}',
                          '-p', Util.getArtifacts(project, configurations.moduleonly, false).values().collect{'${library_directory}/' + it.downloads.artifact.path}.join('${classpath_separator}'),
                          '--add-modules', 'ALL-MODULE-PATH',
                          // Additions to these JVM module args should be mirrored to server_files/args.txt and other similar blocks in the buildscript
                          '--add-opens', 'java.base/java.util.jar=cpw.mods.securejarhandler',
                          '--add-opens', 'java.base/java.lang.invoke=cpw.mods.securejarhandler',
                          '--add-exports', 'java.base/sun.security.util=cpw.mods.securejarhandler',
                          '--add-exports', 'jdk.naming.dns/com.sun.jndi.dns=java.naming'
                    ]
                ],
                libraries: []
            ]


     */

    public abstract Property<String> getId();

    public abstract Property<String> getTime();

    public abstract Property<String> getReleaseTime();

    public abstract Property<String> getType();

    public abstract Property<String> getMainClass();

    public abstract Property<String> getInheritsFrom();

    public abstract Property<Arguments> getArguments();


}
