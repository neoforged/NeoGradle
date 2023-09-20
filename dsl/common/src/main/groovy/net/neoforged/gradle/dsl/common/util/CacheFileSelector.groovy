/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.gradle.dsl.common.util

import groovy.transform.CompileStatic;
import org.gradle.api.tasks.Input;

@CompileStatic
abstract class CacheFileSelector implements Serializable {

    static CacheFileSelector launcherMetadata() {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return "launcher_metadata.json";
            }

            @Override
            String getCacheDirectory() {
                return "./"
            }
        };
    }

    static CacheFileSelector forVersionJson(final String version) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("metadata.json", version);
            }

            @Override
            String getCacheDirectory() {
                return String.format("versions/%s", version);
            }
        };
    }

    static CacheFileSelector forVersionJar(final String version, final String side) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("%s.jar", side);
            }

            @Override
            String getCacheDirectory() {
                return String.format("versions/%s", version);
            }
        };
    }

    static CacheFileSelector forVersionMappings(final String version, final String side) {
        return new CacheFileSelector() {
            @Override
            String getCacheFileName() {
                return String.format("%s.txt", side);
            }

            @Override
            String getCacheDirectory() {
                return String.format("versions/%s", version);
            }
        };
    }

    @Input
    abstract String getCacheFileName();

    @Input
    abstract String getCacheDirectory();

    @Override
    int hashCode() {
        return this.getCacheFileName().hashCode();
    }

    @Override
    boolean equals(Object obj) {
        if (!(obj instanceof CacheFileSelector)) {
            return false;
        }

        final CacheFileSelector cacheFileSelector = (CacheFileSelector) obj;

        return cacheFileSelector.getCacheFileName() == this.getCacheFileName();
    }
}
