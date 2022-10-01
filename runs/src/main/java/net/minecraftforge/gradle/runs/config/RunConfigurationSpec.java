package net.minecraftforge.gradle.runs.config;

import net.minecraftforge.gradle.common.config.VersionedConfiguration;

import java.util.List;
import java.util.Map;

public class RunConfigurationSpec extends VersionedConfiguration {
    private boolean singleInstance;
    private String main;
    private List<String> args;
    private List<String> jvmArgs;
    private boolean client;
    private Map<String, String> env;
    private Map<String, String> props;

    public Boolean getSingleInstance() {
        return singleInstance;
    }

    public void setSingleInstance(Boolean singleInstance) {
        this.singleInstance = singleInstance;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public void setJvmArgs(List<String> jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    public Boolean getClient() {
        return client;
    }

    public void setClient(Boolean client) {
        this.client = client;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }
}
