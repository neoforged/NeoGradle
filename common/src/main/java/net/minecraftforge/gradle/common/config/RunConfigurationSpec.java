package net.minecraftforge.gradle.common.config;

import org.gradle.api.tasks.SourceSet;

import java.util.List;
import java.util.Map;

public class RunConfigurationSpec extends VersionedConfiguration {
    private Boolean singleInstance = null;
    private String taskName;
    private String main;
    private String ideaModule;
    private String workDir;
    private List<SourceSet> sources;
    private List<RunConfigurationSpec> parents;
    private List<RunConfigurationSpec> children;
    private List<String> args, jvmArgs;
    private boolean forceExit = true;
    private Boolean client; // so we can have it null
    private Boolean inheritArgs;
    private Boolean inheritJvmArgs;
    private boolean buildAllProjects;
    private Map<String, String> env;
    private Map<String, String> props;
    private Map<String, String> tokens;

    public Boolean getSingleInstance() {
        return singleInstance;
    }

    public void setSingleInstance(Boolean singleInstance) {
        this.singleInstance = singleInstance;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getMain() {
        return main;
    }

    public void setMain(String main) {
        this.main = main;
    }

    public String getIdeaModule() {
        return ideaModule;
    }

    public void setIdeaModule(String ideaModule) {
        this.ideaModule = ideaModule;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public List<SourceSet> getSources() {
        return sources;
    }

    public void setSources(List<SourceSet> sources) {
        this.sources = sources;
    }

    public List<RunConfigurationSpec> getParents() {
        return parents;
    }

    public void setParents(List<RunConfigurationSpec> parents) {
        this.parents = parents;
    }

    public List<RunConfigurationSpec> getChildren() {
        return children;
    }

    public void setChildren(List<RunConfigurationSpec> children) {
        this.children = children;
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

    public boolean isForceExit() {
        return forceExit;
    }

    public void setForceExit(boolean forceExit) {
        this.forceExit = forceExit;
    }

    public Boolean getClient() {
        return client;
    }

    public void setClient(Boolean client) {
        this.client = client;
    }

    public Boolean getInheritArgs() {
        return inheritArgs;
    }

    public void setInheritArgs(Boolean inheritArgs) {
        this.inheritArgs = inheritArgs;
    }

    public Boolean getInheritJvmArgs() {
        return inheritJvmArgs;
    }

    public void setInheritJvmArgs(Boolean inheritJvmArgs) {
        this.inheritJvmArgs = inheritJvmArgs;
    }

    public boolean isBuildAllProjects() {
        return buildAllProjects;
    }

    public void setBuildAllProjects(boolean buildAllProjects) {
        this.buildAllProjects = buildAllProjects;
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

    public Map<String, String> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, String> tokens) {
        this.tokens = tokens;
    }
}
