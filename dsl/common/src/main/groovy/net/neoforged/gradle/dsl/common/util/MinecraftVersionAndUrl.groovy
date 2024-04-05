package net.neoforged.gradle.dsl.common.util

/**
 * Defines a full minecraft version name and url.
 */
class MinecraftVersionAndUrl {

    private final String version

    private final String url

    MinecraftVersionAndUrl(String version, String url) {
        this.version = version
        this.url = url
    }

    /**
     * The full version name of the minecraft version.
     *
     * @return The full version name of the minecraft version.
     */
    String getVersion() {
        return version
    }

    /**
     * The url to download the minecraft version from.
     *
     * @return The url to download the minecraft version from.
     */
    String getUrl() {
        return url
    }
}
