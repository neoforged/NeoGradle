package net.neoforged.gradle.common.util;

import org.gradle.api.Project;
import org.gradle.api.reflect.ObjectInstantiationException;
import org.gradle.internal.reflect.Instantiator;

import java.util.Arrays;

public class ReflectiveFallbackInstantiator implements Instantiator {

    public static Instantiator create(final Project project) {
        return create(project.getObjects()::newInstance);
    }

    public static Instantiator create(final Instantiator primary) {
        return new ReflectiveFallbackInstantiator(primary);
    }

    private final Instantiator primary;

    private ReflectiveFallbackInstantiator(Instantiator primary) {
        this.primary = primary;
    }

    @Override
    public <T> T newInstance(Class<? extends T> aClass, Object... objects) throws ObjectInstantiationException {
        try {
            return newInstanceFromPrimary(aClass, objects);
        } catch (Throwable throwable) {
            try {
                return newInstanceFromReflection(aClass, objects);
            } catch (Throwable throwable2) {
                throwable.addSuppressed(throwable2);
                throw new ObjectInstantiationException(aClass, throwable);
            }
        }
    }

    private <T> T newInstanceFromPrimary(Class<? extends T> aClass, Object... objects) throws Throwable {
        return primary.newInstance(aClass, objects);
    }

    private <T> T newInstanceFromReflection(Class<? extends T> aClass, Object... objects) throws Throwable {
        final Class<?>[] classesOfObjects = Arrays.stream(objects).map(Object::getClass).toArray(Class[]::new);
        final java.lang.reflect.Constructor<? extends T> constructor = aClass.getConstructor(classesOfObjects);
        return constructor.newInstance(objects);
    }
}
