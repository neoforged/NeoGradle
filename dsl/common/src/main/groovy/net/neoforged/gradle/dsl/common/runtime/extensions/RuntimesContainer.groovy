package net.neoforged.gradle.dsl.common.runtime.extensions

import groovy.transform.CompileStatic
import net.neoforged.gradle.dsl.common.runtime.definition.Definition
import net.neoforged.gradle.dsl.common.runtime.spec.Specification
import net.neoforged.gradle.dsl.common.runtime.spec.TaskTreeBuilder
import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider

@CompileStatic
interface RuntimesContainer extends NamedDomainObjectContainer<Definition> {

    Definition register(
            Specification specification,
            TaskTreeBuilder builder
    ) throws InvalidUserDataException;
}
