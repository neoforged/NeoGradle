package net.minecraftforge.gradle.mcp.tasks;

import net.minecraftforge.gradle.common.tasks.DownloadingTask;
import net.minecraftforge.gradle.common.util.HashFunction;
import net.minecraftforge.gradle.common.util.TransformerUtils;
import net.minecraftforge.gradle.common.util.Utils;
import net.minecraftforge.gradle.mcp.extensions.McpMinecraftExtension;
import net.minecraftforge.gradle.mcp.naming.NamingChannelProvider;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class MojangLicenseTask extends DownloadingTask {

    public static final String HIDE_LICENSE = "hideOfficialWarningUntilChanged";
    public static final String SHOW_LICENSE = "reshowOfficialWarning";
    public static final String EMIT_LICENSE = "emitOfficialWarning";


    public abstract static class Display extends MojangLicenseTask {
        public Display() {
            getProject().getTasks().configureEach(task -> {
                if (task instanceof Display)
                    return;

                task.dependsOn(this);
            });
        }

        @TaskAction
        public void run() {
            displayWarning(getMappingChannel().get(), getMappingVersion().get());

            if (!getUpdateChannel().isPresent() || getUpdateChannel().get().equals(getMappingChannel().get())) return;

            displayWarning(getUpdateChannel().get(), getUpdateVersion().getOrElse(getMappingVersion().get()));
        }

        private void displayWarning(NamingChannelProvider channel, @Nullable Map<String, String> version) {
            if ("official".equals(channel.getName())) {
                Provider<String> license = version != null ? getOfficialLicense(channel.getMinecraftVersionExtractor().get().produce(version)) : getProject().provider(() -> null);
                Provider<String> licenseHash = license.map(HashFunction.SHA1::hash);

                if (licenseHash.isPresent() && isHidden(licenseHash.get())) return;

                String warning = buildWarning(license.getOrNull());

                getProject().getLogger().warn(warning);
                if (license.isPresent()) {
                    getProject().getLogger().warn(Arrays.stream(license.get().split("\n")).map("WARNING: %s"::formatted).collect(Collectors.joining("\n")));
                }
            }
        }

        private String buildWarning(@Nullable String license) {
            String warning = """
                WARNING: This project is configured to use the official obfuscation mappings provided by Mojang.
                WARNING: These mapping fall under their associated license, you should be fully aware of this license.
                WARNING: For the latest license text, refer {REFER}, or the reference copy here: https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md.
                WARNING: You can hide this warning by running the `{TASK}` task
                """;

            return warning
                    .replace("{REFER}", license != null ? "below" : "to the mapping file itself")
                    .replace("{TASK}", HIDE_LICENSE);
        }

        @Input
        @Optional
        public abstract Property<NamingChannelProvider> getUpdateChannel();

        @Input
        @Optional
        public abstract MapProperty<String, String> getUpdateVersion();
    }

    public abstract static class Show extends MojangLicenseTask {

        @TaskAction
        public void run() {
            if (!"official".equals(getMappingChannel().get().getName())) return;

            Provider<String> hash = getOfficialLicense(getMappingChannel().get().getMinecraftVersionExtractor().get().produce(getMappingVersion().get())).map(HashFunction.SHA1::hash);

            Path accepted = getLicensePath(hash.get());

            Utils.delete(accepted.toFile());
        }
    }

    public abstract static class Hide extends MojangLicenseTask {

        @TaskAction
        public void run() {
            if (!"official".equals(getMappingChannel().get().getName())) return;

            Provider<String> hash = getOfficialLicense(getMappingChannel().get().getMinecraftVersionExtractor().get().produce(getMappingVersion().get())).map(HashFunction.SHA1::hash);


            Path accepted = getLicensePath(hash.get());

            if (Files.exists(accepted)) return;

            try {
                Utils.createEmpty(accepted.toFile());

                String warning = "WARNING: These warnings will not be shown again until the license changes "
                        + "or the task `{TASK}` is run.";

                getProject().getLogger().warn(warning.replace("{TASK}", SHOW_LICENSE));
            } catch (IOException exception) {
                getProject().getLogger().error("Could not accept Mojang license", exception);
            }
        }
    }

    public MojangLicenseTask() {
        getMappingChannel().convention(
                getProject().getExtensions().getByType(McpMinecraftExtension.class).getMappings().getMappingChannel()
        );
        getMappingVersion().convention(
                getProject().getExtensions().getByType(McpMinecraftExtension.class).getMappings().getMappingVersion()
        );
    }

    protected boolean isHidden(String hash) {
        return Files.exists(getLicensePath(hash));
    }

    protected Path getLicensePath(String hash) {
        return Objects.requireNonNull(getProject().getGradle().getGradleHomeDir()).toPath().resolve("caches").resolve("minecraft").resolve("licenses").resolve(hash);
    }

    protected Provider<String> getOfficialLicense(String version) {
        String artifact = "net.minecraft:client:" + version + ":mappings@txt";

        Provider<File> mappingsFile = getDownloader().flatMap(d -> d.generate(artifact, true));

        if (!mappingsFile.isPresent()) return getProject().provider(() -> null);

        return mappingsFile.map(
                TransformerUtils.guardWithResource(
                        t -> t.filter(s -> s.startsWith("#"))
                                .map(s -> s.substring(1).trim())
                                .collect(Collectors.joining("\n")),
                        f -> Files.lines(f.toPath())
                )
        );
    }

    @Input
    public abstract Property<NamingChannelProvider> getMappingChannel();

    @Input
    public abstract MapProperty<String, String> getMappingVersion();
}
