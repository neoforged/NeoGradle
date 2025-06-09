package net.neoforged.gradle.eclipse;

import java.io.File;

public interface IPropertyDelegate<T>
{
    T get();

    void set(T t);

    void convention(T provider);
}
