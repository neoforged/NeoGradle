package net.minecraftforge.gradle.common

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.common.transform.DSLProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

@CompileStatic
@SuppressWarnings('unused')
interface TestExtension {
    @DSLProperty(factory = { new HelloWorld() })
    abstract Property<HelloWorld> getHelloWorld()

    @DSLProperty(factory = { new ListObject() })
    abstract ListProperty<ListObject> getListObjects()

    static final class HelloWorld {
        String thing
        int thing2
    }

    static final class ListObject {
        String name
    }
}
