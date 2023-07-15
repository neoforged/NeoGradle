package net.neoforged.gradle.common.runtime.naming.renamer;

import java.io.Serializable;

public interface ITypeRenamer extends Serializable {

    /**
     * Renames a type.
     *
     * @param type The type to rename.
     * @return The renamed type.
     */
    String renameType(String type);

    /**
     * Renames a field.
     *
     * @param owner The owner of the field.
     * @param name The name of the field.
     * @return The renamed field.
     */
    String renameField(String owner, String name);

    /**
     * Renames a method.
     *
     * @param owner The owner of the method.
     * @param name The name of the method.
     * @param desc The descriptor of the method.
     * @return The renamed method.
     */
    String renameMethod(String owner, String name, String desc);

    /**
     * Renames a descriptor
     *
     * @param desc The descriptor to rename.
     * @return The renamed descriptor.
     */
    String renameDescriptor(String desc);
}
