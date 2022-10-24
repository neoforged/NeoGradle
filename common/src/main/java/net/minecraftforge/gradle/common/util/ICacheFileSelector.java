package net.minecraftforge.gradle.common.util;

import org.gradle.api.tasks.Input;

import java.io.Serializable;

public abstract class ICacheFileSelector implements Serializable {

    public static ICacheFileSelector launcherMetadata() {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return "launcher_metadata.json";
            }
        };
    }

    public static ICacheFileSelector forVersionJson(final String version) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return String.format("versions/%s/metadata.json", version);
            }
        };
    }

    public static ICacheFileSelector forVersionJar(final String version, final String side) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return String.format("versions/%s/%s.jar", version, side);
            }
        };
    }

    public static ICacheFileSelector forVersionMappings(final String version, final String side) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return String.format("versions/%s/%s.txt", version, side);
            }
        };
    }

    @Input
    public abstract String getCacheFileName();

    @Override
    public int hashCode() {
        return this.getCacheFileName().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ICacheFileSelector)) {
            return false;
        }

        final ICacheFileSelector cacheFileSelector = (ICacheFileSelector) obj;

        return cacheFileSelector.getCacheFileName().equals(this.getCacheFileName());
    }
}
