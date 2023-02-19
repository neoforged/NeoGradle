package net.minecraftforge.gradle.vanilla.util;

public final class InterpolationConstants {

    private InterpolationConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: InterpolationConstants. This is a utility class");
    }

    public static final String VERSION_NAME = "version_name";
    public static final String GAME_DIRECTORY = "game_directory";
    public static final String ASSETS_ROOT = "assets_root";
    public static final String ASSETS_INDEX_NAME = "assets_index_name";

    public static final String AUTH_ACCESS_TOKEN = "auth_access_token";
    public static final String USER_TYPE = "user_type";
    public static final String VERSION_TYPE = "version_type";

    public static final String NATIVES_DIRECTORY = "natives_directory";
    public static final String LAUNCHER_NAME = "launcher_name";
    public static final String LAUNCHER_VERSION = "launcher_version";
}
