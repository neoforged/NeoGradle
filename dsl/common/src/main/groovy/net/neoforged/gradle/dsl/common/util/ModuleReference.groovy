package net.neoforged.gradle.dsl.common.util

import org.jetbrains.annotations.Nullable

class ModuleReference {

    @Nullable
    private final String group;
    private final String name;
    @Nullable
    private final String version;
    @Nullable
    private final String extension;
    @Nullable
    private final String classifier;

    ModuleReference(@Nullable String group, String name, @Nullable String version, @Nullable String extension, @Nullable String classifier) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.extension = extension;
        this.classifier = classifier;
    }

    @Nullable
    String getGroup() {
        return group;
    }

    String getName() {
        return name;
    }

    @Nullable
    String getVersion() {
        return version;
    }

    @Nullable
    String getExtension() {
        return extension;
    }

    @Nullable
    String getClassifier() {
        return classifier;
    }

    boolean equals(o) {
        if (this.is(o)) return true
        if (o == null || getClass() != o.class) return false

        ModuleReference reference = (ModuleReference) o

        if (classifier != reference.classifier) return false
        if (extension != reference.extension) return false
        if (group != reference.group) return false
        if (name != reference.name) return false
        if (version != reference.version) return false

        return true
    }

    int hashCode() {
        int result
        result = (group != null ? group.hashCode() : 0)
        result = 31 * result + (name != null ? name.hashCode() : 0)
        result = 31 * result + (version != null ? version.hashCode() : 0)
        result = 31 * result + (extension != null ? extension.hashCode() : 0)
        result = 31 * result + (classifier != null ? classifier.hashCode() : 0)
        return result
    }


    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        if (getGroup() != null)
            builder.append(getGroup())

        builder.append(":").append(getName()).append(":")

        if (getVersion() != null)
            builder.append(getVersion())
        else
            builder.append("+")

        if (getClassifier() != null)
            builder.append(":").append(getClassifier())

        if (getExtension() != null && getExtension() != "jar")
            builder.append("@").append(getExtension())

        return builder;
    }
}
