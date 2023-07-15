package net.neoforged.gradle.vanilla.util;

import net.neoforged.gradle.common.util.BundledServerUtils;

import java.io.File;

public class ServerLaunchInformation {

    public static ServerLaunchInformation from(final File serverFile) {
        if (BundledServerUtils.isBundledServer(serverFile)) {
            return new ServerLaunchInformation(BundledServerUtils.getBundledMainClass(serverFile), true);
        }

        //TODO: Auto extract this from the manifest, but for now this will do.
        return new ServerLaunchInformation("net.minecraft.server.Main", false);
    }

    private final String mainClass;
    private final boolean isBundledServer;

    private ServerLaunchInformation(String mainClass, boolean isBundledServer) {
        this.mainClass = mainClass;
        this.isBundledServer = isBundledServer;
    }

    public String getMainClass() {
        return mainClass;
    }

    public boolean isBundledServer() {
        return isBundledServer;
    }
}
