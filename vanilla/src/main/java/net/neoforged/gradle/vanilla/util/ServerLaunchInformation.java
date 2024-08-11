package net.neoforged.gradle.vanilla.util;

import net.neoforged.gradle.common.util.BundledServerUtils;
import net.neoforged.gradle.dsl.common.tasks.WithOutput;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

public class ServerLaunchInformation {

    public static ServerLaunchInformation from(final TaskProvider<? extends WithOutput> serverFile) {
        final Provider<Boolean> isBundled = serverFile.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(BundledServerUtils::isBundledServer);

        final Provider<String> mainClass = serverFile.flatMap(WithOutput::getOutput).map(RegularFile::getAsFile).map(BundledServerUtils::getBundledMainClass)
                .orElse("net.minecraft.server.Main");

        return new ServerLaunchInformation(
                mainClass,
                isBundled
        );
    }

    private final Provider<String> mainClass;
    private final Provider<Boolean> isBundledServer;

    private ServerLaunchInformation(Provider<String> mainClass, Provider<Boolean> isBundledServer) {
        this.mainClass = mainClass;
        this.isBundledServer = isBundledServer;
    }

    public Provider<String> getMainClass() {
        return mainClass;
    }

    public Provider<Boolean> isBundledServer() {
        return isBundledServer;
    }
}
