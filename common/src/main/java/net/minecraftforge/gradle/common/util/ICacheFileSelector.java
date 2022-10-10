package net.minecraftforge.gradle.common.util;

import org.gradle.api.tasks.Input;

import java.io.Serializable;

public interface ICacheFileSelector extends Serializable {

    static ICacheFileSelector launcherMetadata() {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return "launcher_metadata.json";
            }

            @Override
            public int hashCode() {
                return this.getCacheFileName().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }

                if (!(obj instanceof ICacheFileSelector cacheFileSelector)) {
                    return false;
                }

                return cacheFileSelector.getCacheFileName().equals(this.getCacheFileName());
            }
        };
    }

    static ICacheFileSelector forVersionJson(final String version) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return "versions/%s/metadata.json".formatted(version);
            }

            @Override
            public int hashCode() {
                return this.getCacheFileName().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }

                if (!(obj instanceof ICacheFileSelector cacheFileSelector)) {
                    return false;
                }

                return cacheFileSelector.getCacheFileName().equals(this.getCacheFileName());
            }
        };
    }

    static ICacheFileSelector forVersionJar(final String version, final String side) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return "versions/%s/%s.jar".formatted(version, side);
            }

            @Override
            public int hashCode() {
                return this.getCacheFileName().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }

                if (!(obj instanceof ICacheFileSelector cacheFileSelector)) {
                    return false;
                }

                return cacheFileSelector.getCacheFileName().equals(this.getCacheFileName());
            }

        };
    }

    static ICacheFileSelector forVersionMappings(final String version, final String side) {
        return new ICacheFileSelector() {
            @Override
            public String getCacheFileName() {
                return "versions/%s/%s.txt".formatted(version, side);
            }

            @Override
            public int hashCode() {
                return this.getCacheFileName().hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }

                if (!(obj instanceof ICacheFileSelector cacheFileSelector)) {
                    return false;
                }

                return cacheFileSelector.getCacheFileName().equals(this.getCacheFileName());
            }
        };
    }

    @Input
    String getCacheFileName();
}
