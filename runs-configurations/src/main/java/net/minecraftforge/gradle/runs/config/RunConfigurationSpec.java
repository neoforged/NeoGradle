package net.minecraftforge.gradle.runs.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraftforge.gradle.common.config.VersionedConfiguration;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "FieldMayBeFinal", "FieldCanBeLocal"}) //This is a configuration specification class, stuff is defined here with defaults.
public class RunConfigurationSpec extends VersionedConfiguration {
    private boolean singleInstance = false;
    private String main;
    private List<String> args = Lists.newArrayList();
    private List<String> jvmArgs = Lists.newArrayList();
    private boolean client = true;
    private Map<String, String> env = Maps.newHashMap();
    private Map<String, String> props = Maps.newHashMap();

    /**
     * {@return Indicates if this run configuration can be started multiple times simultaneously.}
     */
    public boolean getSingleInstance() {
        return singleInstance;
    }

    /**
     * {@return The main class to run.}
     */
    public String getMain() {
        return main;
    }

    /**
     * {@return The arguments to pass to the main class.}
     */
    public List<String> getProgramArguments() {
        return args;
    }

    /**
     * {@return The arguments to pass to the JVM.}
     */
    public List<String> getJvmArguments() {
        return jvmArgs;
    }

    /**
     * {@return Indicates if this run configuration is a client.}
     */
    public boolean getClient() {
        return client;
    }

    /**
     * {@return The environment variables to set.}
     */
    public Map<String, String> getEnvironmentVariables() {
        return env;
    }

    /**
     * {@return The system properties to set.}
     */
    public Map<String, String> getProperties() {
        return props;
    }
}
