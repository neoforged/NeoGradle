/*
 * ForgeGradle
 * Copyright (C) 2018 Forge Development LLC
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 */

package net.neoforged.gradle.dsl.common.util

import com.google.common.base.Splitter
import com.google.common.collect.ComparisonChain
import com.google.common.collect.Iterables
import groovy.transform.CompileStatic
import org.apache.maven.artifact.versioning.ComparableVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyArtifact
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.specs.Spec

import javax.annotation.Nullable
import java.util.function.Predicate

@CompileStatic
class Artifact implements Comparable<Artifact>, Serializable {

    // group:name:version[:classifier][@extension]
    private final String group;
    private final String name;
    private final String version;
    @Nullable
    private final String classifier;
    @Nullable
    private final String ext;

    // Cached after building the first time we're asked
    // Transient field so these aren't serialized
    @Nullable
    private transient String path;
    @Nullable
    private transient String file;
    @Nullable
    private transient String fullDescriptor;
    @Nullable
    private transient ComparableVersion comparableVersion;
    @Nullable
    private transient Boolean isSnapshot;

    static Artifact from(String descriptor) {
        String group, name, version;
        String ext = null, classifier = null;

        String[] pts = Iterables.toArray(Splitter.on(':').split(descriptor), String.class);
        group = pts[0];
        name = pts[1];

        int last = pts.length - 1;
        int idx = pts[last].indexOf('@');
        if (idx != -1) { // we have an extension
            ext = pts[last].substring(idx + 1);
            pts[last] = pts[last].substring(0, idx);
        }

        version = pts[2];

        if (pts.length > 3) // We have a classifier
            classifier = pts[3];

        return new Artifact(group, name, version, classifier, ext);
    }

    static Artifact from(String group, String name, String version, @Nullable String classifier, @Nullable String ext) {
        return new Artifact(group, name, version, classifier, ext);
    }

    static Artifact from(Project project, @Nullable String classifier, @Nullable String ext) {
        return new Artifact(project.getGroup().toString(), project.getName().toString(), project.getVersion().toString(), classifier, ext);
    }


    static Artifact from(final Dependency dependency) {
        if (dependency instanceof ExternalModuleDependency) {
            final ExternalModuleDependency externalModuleDependency = (ExternalModuleDependency) dependency;
            return from(externalModuleDependency);
        }

        throw new IllegalArgumentException(String.format("Dependency: %s is not an external Artifact that can be referenced.", dependency));
    }

    static Artifact from(ExternalModuleDependency dependency) {
        if (dependency.getArtifacts().isEmpty()) {
            return new Artifact(dependency.getGroup() == null ? "" : dependency.getGroup(), dependency.getName(), dependency.getVersion() == null ? "" : dependency.getVersion(), null, null);
        }

        DependencyArtifact artifact = dependency.getArtifacts().iterator().next();
        return new Artifact(dependency.getGroup() == null ? "" : dependency.getGroup(), dependency.getName(), dependency.getVersion() == null ? "" : dependency.getVersion(), artifact.getClassifier(), artifact.getExtension());
    }

    static Artifact from(ResolvedArtifact dependency) {
        return new Artifact(dependency.moduleVersion.id.group == null ? "" : dependency.moduleVersion.id.group,
                dependency.moduleVersion.id.name,
                dependency.moduleVersion.id.version == null ? "" : dependency.moduleVersion.id.version,
                dependency.classifier,
                dependency.extension);
    }

    Artifact(String group, String name, String version, @Nullable String classifier, @Nullable String ext) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.ext = ext != null ? ext : "jar";
    }

    String getLocalPath() {
        return getPath().replace('/' as char, File.separatorChar);
    }

    String getDescriptor() {
        if (fullDescriptor == null) {
            StringBuilder buf = new StringBuilder();
            buf.append(this.group).append(':').append(this.name).append(':').append(this.version);
            if (this.classifier != null) {
                buf.append(':').append(this.classifier);
            }
            if (ext != null && "jar" != this.ext) {
                buf.append('@').append(this.ext);
            }
            this.fullDescriptor = buf.toString();
        }
        return fullDescriptor;
    }

    String getPath() {
        if (path == null) {
            this.path = String.join("/", this.group.replace('.', '/'), this.name, this.version, getFilename());
        }
        return path;
    }

    String getGroup() {
        return group;
    }

    String getName() {
        return name;
    }

    String getVersion() {
        return version;
    }

    @Nullable
    String getClassifier() {
        return classifier;
    }

    @Nullable
    String getExtension() {
        return ext;
    }

    String getFilename() {
        if (file == null) {
            String file;
            file = this.name + '-' + this.version;
            if (this.classifier != null) file += '-' + this.classifier;
            file += '.' + this.ext;
            this.file = file;
        }
        return file;
    }

    boolean isSnapshot() {
        if (isSnapshot == null) {
            this.isSnapshot = this.version.toLowerCase(Locale.ROOT).endsWith("-snapshot");
        }
        return isSnapshot;
    }

    Artifact withVersion(String version) {
        return from(group, name, version, classifier, ext);
    }

    @Override
    String toString() {
        return getDescriptor();
    }

    @Override
    int hashCode() {
        return getDescriptor().hashCode();
    }

    @Override
    boolean equals(Object o) {
        return o instanceof Artifact &&
                this.getDescriptor().equals(((Artifact) o).getDescriptor());
    }

    Spec<Dependency> asDependencySpec() {
        return (Dependency dep) -> group.equals(dep.getGroup()) && name.equals(dep.getName()) && version.equals(dep.getVersion());
    }

    Predicate<ResolvedArtifact> asArtifactMatcher() {
        return (ResolvedArtifact art) -> {
            String theirClassifier;
            if (art.getClassifier() == null) {
                theirClassifier = "";
            } else {
                theirClassifier = art.getClassifier();
            }

            String theirExt;
            if (art.getExtension().isEmpty()) {
                theirExt = "jar";
            } else {
                theirExt = art.getExtension();
            }

            return (classifier == null || classifier.equals(theirClassifier)) && (ext == null || ext.equals(theirExt));
        };
    }

    ComparableVersion getComparableVersion() {
        if (comparableVersion == null) {
            this.comparableVersion = new ComparableVersion(this.version);
        }
        return comparableVersion;
    }

    @Override
    int compareTo(Artifact o) {
        return ComparisonChain.start()
                .compare(group, o.group)
                .compare(name, o.name)
                .compare(getComparableVersion(), o.getComparableVersion())
                // TODO: comparison of timestamps for snapshot versions (isSnapshot)
                .compare(classifier, o.classifier, Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(ext, o.ext, Comparator.nullsFirst(Comparator.naturalOrder()))
                .result();
    }

    Dependency toDependency(Project project) {
        return project.getDependencies().create(getDescriptor());
    }
}
