package net.neoforged.gradle.common.extensions;

import groovy.lang.MissingPropertyException;
import org.gradle.api.Project;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class ExtensionManager {

    public static final String EXTENSION_CHECK_PROPERTY_NAME = "forgegradle.extensions.%s.type";

    private final Project project;

    @Inject
    public ExtensionManager(Project project) {
        this.project = project;
    }

    @SuppressWarnings("unchecked")
    public <T> void registerExtension(String name, Class<T> publicFacingType, IExtensionCreator<T> defaultCreator) {
        final Object projectExtensionTypeProperty;
        try {
            projectExtensionTypeProperty = project.findProperty(String.format(EXTENSION_CHECK_PROPERTY_NAME, name));
        } catch (MissingPropertyException missingPropertyException) {
            project.getExtensions().add(publicFacingType, name, defaultCreator.apply(project));
            return;
        }

        if (projectExtensionTypeProperty == null) {
            project.getExtensions().add(publicFacingType, name, defaultCreator.apply(project));
            return;
        }

        if (projectExtensionTypeProperty instanceof IExtensionCreator) {
            try {
                final IExtensionCreator<T> overrideCreator = (IExtensionCreator<T>) projectExtensionTypeProperty;
                project.getExtensions().add(publicFacingType, name, overrideCreator.apply(project));
                return;
            } catch (ClassCastException classCastException) {
                throw new IllegalArgumentException(String.format("Property '%s' is not a valid extension creator for type: %s", String.format(EXTENSION_CHECK_PROPERTY_NAME, name), publicFacingType.getName()), classCastException);
            }
        }

        if (projectExtensionTypeProperty instanceof String) {
            final String overrideCreatorName = (String) projectExtensionTypeProperty;
            try {
                final Class<?> overrideCreatorClass = Class.forName(overrideCreatorName);
                final Constructor<?> overrideCreatorConstructor = overrideCreatorClass.getConstructor();
                overrideCreatorConstructor.setAccessible(true);
                final Object overrideCreatorCandidate = overrideCreatorConstructor.newInstance();

                if (!(overrideCreatorCandidate instanceof IExtensionCreator))
                    throw new IllegalArgumentException(String.format("Object of type '%s', returned by property '%S' is not a extension creator.", overrideCreatorClass.getName(), String.format(EXTENSION_CHECK_PROPERTY_NAME, name)));

                final IExtensionCreator<T> overrideCreator = (IExtensionCreator<T>) overrideCreatorCandidate;
                project.getLogger().warn("Using Extension Creator Candidate: " + overrideCreatorName + " for extension: " + name + " of type: " + publicFacingType.getName() + ".");
                project.getExtensions().add(publicFacingType, name, overrideCreator.apply(project));
                return;
            } catch (ClassCastException classCastException) {
                throw new IllegalArgumentException(String.format("Property '%s' is not a valid extension creator for type: %s", String.format(EXTENSION_CHECK_PROPERTY_NAME, name), publicFacingType.getName()), classCastException);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(String.format("Property '%s' targets an unknown class: '%s', so it can not be used as a extension creator", String.format(EXTENSION_CHECK_PROPERTY_NAME, name), overrideCreatorName), e);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(String.format("Extension Creator Candidate: '%s' has no public no-args constructor. As such it can not be used as a Extension Creator.", overrideCreatorName), e);
            } catch (InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to invoke Extension Creator Candidate: " + overrideCreatorName, e);
            } catch (InstantiationException e) {
                throw new IllegalArgumentException("Failed to instantiate Extension Creator Candidate: " + overrideCreatorName, e);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Failed to access Extension Creator Candidate: " + overrideCreatorName, e);
            }
        }

        throw new IllegalArgumentException("Property '" + String.format(EXTENSION_CHECK_PROPERTY_NAME, name) + "' is not a valid extension creator. It must be either a string of a class name implementing IExtensionCreator, or an instance of IExtensionCreator.");
    }

    public static void registerOverride(final Project project, final String name, final IExtensionCreator<?> creator) {
        project.getExtensions().add(String.format(EXTENSION_CHECK_PROPERTY_NAME, name), creator);
    }
}
