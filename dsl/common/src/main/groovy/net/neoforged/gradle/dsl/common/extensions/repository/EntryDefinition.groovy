package net.neoforged.gradle.dsl.common.extensions.repository

import net.minecraftforge.gdi.BaseDSLElement

/**
 * Defines an entry for a dummy repository.
 */
interface EntryDefinition extends BaseDSLElement<EntryDefinition> {

    /**
     * Creates a new entry from the given builder, using this definition.
     *
     * @param builder The builder to create the entry from.
     * @return The created entry.
     */
    Entry createFrom(Entry.Builder builder)
}