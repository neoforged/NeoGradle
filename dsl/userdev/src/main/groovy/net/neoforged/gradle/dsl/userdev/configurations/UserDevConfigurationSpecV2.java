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

package net.neoforged.gradle.dsl.userdev.configurations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.neoforged.gradle.dsl.neoform.configuration.NeoFormConfigConfigurationSpecV1;

import java.nio.charset.StandardCharsets;
import java.util.*;

@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"}) //This is a configuration specification class, stuff is defined here with defaults.
public class UserDevConfigurationSpecV2 extends UserDevConfigurationSpecV1 {

    private DataFunction processor;
    private String patchesOriginalPrefix;
    private String patchesModifiedPrefix;
    private boolean notchObf = false;
    private List<String> universalFilters = Lists.newArrayList();
    private List<String> modules = Lists.newArrayList();
    private String sourceFileCharset = StandardCharsets.UTF_8.name();

    /**
     * {@return The post data based post processor for the userdev configuration.}
     */
    public DataFunction getPostProcessor() {
        return processor;
    }

    /**
     * {@return The line prefix of the original file markers in source patch files}
     */
    public String getPatchesOriginalPrefix() {
        return patchesOriginalPrefix;
    }

    /**
     * {@return The line prefix of the modified file markers in source patch files}
     */
    public String getPatchesModifiedPrefix() {
        return patchesModifiedPrefix;
    }

    /**
     * {@return If the userdev configuration expects notch obfuscated names.}
     */
    public boolean isNotchObf() {
        return notchObf;
    }

    /**
     * {@return The filters which are used during resource merging from the universal file to the userdev runtime.}
     */
    public List<String> getUniversalFilters() {
        return universalFilters;
    }

    /**
     * {@return The modules passed to --module-path during compilation.}
     */
    public List<String> getModules() {
        return modules;
    }

    /**
     * {@return The charset used to read source files.}
     */
    public String getSourceFileCharset() {
        return sourceFileCharset;
    }

    /**
     * Defines a runtime execution function that can be used with additional data.
     */
    public static class DataFunction extends NeoFormConfigConfigurationSpecV1.Function {
        private Map<String, String> data = Maps.newHashMap();

        /**
         * {@return The additional data passed to the function.}
         */
        public Map<String, String> getData() {
            return data;
        }
    }
}
