package net.neoforged.gradle.common.dependency;

import groovy.lang.Closure;
import net.neoforged.gradle.dsl.common.dependency.DependencyManagementObject;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;

import javax.inject.Inject;
import java.util.Map;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractDependencyManagementObject implements DependencyManagementObject {
    @Inject
    protected abstract DependencyFactory getDependencyFactory();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @SuppressWarnings("unchecked")
    public Spec<? super ModuleComponentIdentifier> dependency(Object notation) {
        Dependency dependency;
        if (notation instanceof Dependency) {
            dependency = (Dependency) notation;
        } else if (notation instanceof CharSequence) {
            dependency = getDependencyFactory().create((CharSequence) notation);
        } else if (notation instanceof Project) {
            dependency = getDependencyFactory().create((Project) notation);
        } else if (notation instanceof FileCollection) {
            dependency = getDependencyFactory().create((FileCollection) notation);
        } else if (notation instanceof Map) {
            Map<String, String> map = (Map<String, String>) notation;
            String group = map.get("group");
            String name = map.get("name");
            String version = map.get("version");
            String classifier = map.get("classifier");
            String ext = map.get("extension");
            dependency = getDependencyFactory().create(group, name, version, ext, classifier);
        } else {
            throw new IllegalArgumentException("Cannot convert " + notation + " to a Dependency");
        }
        return dependency(dependency);
    }

    public Spec<? super ModuleComponentIdentifier> dependency(Dependency dependency) {
        Provider<String> groupProvider = getProviderFactory().provider(dependency::getGroup);
        Provider<String> nameProvider = getProviderFactory().provider(dependency::getName);
        Provider<String> versionProvider = getProviderFactory().provider(dependency::getVersion);

        return this.dependency(new Closure<Boolean>(null) {

            @SuppressWarnings("ConstantConditions")
            @Override
            public Boolean call(final Object it) {
                if (it instanceof ModuleComponentIdentifier) {
                    final ModuleComponentIdentifier identifier = (ModuleComponentIdentifier) it;
                    return (groupProvider.get() == null || Pattern.matches(groupProvider.get(), identifier.getGroup())) &&
                            (nameProvider.get() == null || Pattern.matches(nameProvider.get(), identifier.getModule())) &&
                            (versionProvider.get() == null || Pattern.matches(versionProvider.get(), identifier.getVersion()));
                }

                return false;
            }
        });
    }

    public Spec<? super ModuleComponentIdentifier> dependency(Closure<Boolean> spec) {
        return Specs.convertClosureToSpec(spec);
    }
}
