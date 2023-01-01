/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.minecraftforge.gradle.dsl.userdev.configurations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.gradle.dsl.common.configuration.VersionedConfiguration;
import net.minecraftforge.gradle.dsl.mcp.configuration.McpConfigConfigurationSpecV1;
import net.minecraftforge.gradle.dsl.runs.type.Type;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"}) //This is a configuration specification class, stuff is defined here with defaults.
public class UserDevConfigurationSpecV1 extends VersionedConfiguration {

    @Nullable
    private String mcp;    // Do not specify this unless there is no parent.
    @Nullable
    private String parent; // To fully resolve, we must walk the parents until we hit null, and that one must specify a MCP value.
    private List<String> ats = Lists.newArrayList();
    private List<String> sass = Lists.newArrayList();
    private List<String> srgs = Lists.newArrayList();
    private List<String> srg_lines = Lists.newArrayList();
    private String binpatches; //To be applied to joined.jar, remapped, and added to the classpath
    private McpConfigConfigurationSpecV1.Function binpatcher;
    private String patches;
    @Nullable
    private String sources;
    @Nullable
    private String universal; //Remapped and added to the classpath, Contains new classes and resources
    private List<String> libraries = Lists.newArrayList();
    @Nullable
    private String inject;
    private Map<String, Type> runs = Maps.newHashMap();
    private String sourceCompatibility = "1.8";
    private String targetCompatibility = "1.8";


    /**
     * {@return An optional potentially containing the mcp version this configuration inherits from.}
     * @implNote The optional is only populated if the optional returned by {@link #getParentName()} is empty.
     */
    public Optional<String> getMcpVersion() {
        return Optional.ofNullable(mcp);
    }

    /**
     * {@return An optional potentially containing the name of the parent configuration that this configuration inherits from.}
     * @implNote The optional is only populated if the optional returned by {@link #getMcpVersion()} is empty.
     */
    public Optional<String> getParentName() {
        return Optional.ofNullable(parent);
    }

    /**
     * {@return The list of additional access transformer files contained in the userdev artifact which should be applied together with the user specified ones, and the ones from the parent configuration.}
     */
    public List<String> getAccessTransformerPaths() {
        return ats;
    }

    /**
     * {@return The list of the additional side annotation stripper files contained in the userdev artifact which should be applied together with the user specified ones, and the ones from the parent configuration.}
     */
    public List<String> getSideAnnotationStripperPaths() {
        return sass;
    }

    /**
     * {@return The list of additional srg based mapping files contained in the userdev artifact which should be applied together with the user specified ones, and the ones from the parent configuration.}
     */
    public List<String> getExtraMappingPaths() {
        return srgs;
    }

    /**
     * {@return The list of additional srg based mappings which should be applied together with the user specified ones, and the ones from the parent configuration.}
     */
    public List<String> getExtraMappingEntries() {
        return srg_lines;
    }

    /**
     * {@return The defined file here is given to the binary patcher to apply to the raw jar only.}
     */
    public String getBinaryPatchFile() {
        return binpatches;
    }

    /**
     * {@return The function specification to use to handle the binary patching of the raw jar. Also known as the binary patcher.}
     */
    public McpConfigConfigurationSpecV1.Function getBinaryPatcher() {
        return binpatcher;
    }

    /**
     * {@return The path to the directory with source level patches to apply to the remapped to srg jar.}
     */
    public String getSourcePatchesDirectory() {
        return patches;
    }

    /**
     * {@return An optional potentially containing the coordinate of the sources artifact}
     */
    public Optional<String> getSourcesArtifactIdentifier() {
        return Optional.ofNullable(sources);
    }

    /**
     * {@return An optional potentially containing the coordinate of the universal artifact}
     */
    public Optional<String> getUniversalArtifactIdentifier() {
        return Optional.ofNullable(universal);
    }

    /**
     * {@return The list of additional libraries to add to the classpath}
     */
    public List<String> getAdditionalDependencies() {
        return libraries;
    }

    /**
     * {@return An optional which potentially contains the directory of files to inject into the runtime.}
     */
    public Optional<String> getInjectedFilesDirectory() {
        return Optional.ofNullable(inject);
    }

    /**
     * {@return The map of run configuration specifications that this configuration defines.}
     */
    public Map<String, Type> getRunConfigurationSpecifications() {
        return runs;
    }

    /**
     * {@return The source compatibility level to use for the java compiler.}
     */
    public String getSourceCompatibility() {
        return sourceCompatibility;
    }

    /**
     * {@return The target compatibility level to use for the java compiler.}
     */
    public String getTargetCompatibility() {
        return targetCompatibility;
    }
}
