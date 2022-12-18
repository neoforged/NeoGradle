package net.minecraftforge.gradle.common

import groovy.transform.CompileStatic
import net.minecraftforge.gradle.common.transform.DSLProperty
import net.minecraftforge.gradle.common.transform.ProjectGetter
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

import javax.inject.Inject

@CompileStatic
@SuppressWarnings('unused')
interface TestExtension {
    @Inject
    abstract ObjectFactory getFactory()

    @DSLProperty(factory = { getFactory().newInstance(DSLYesImpl) })
    abstract ListProperty<DSLYes> getYeses()

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

@CompileStatic
interface DSLYes {
    @DSLProperty
    abstract ListProperty<String> getStrings()
}

@CompileStatic
abstract class DSLYesImpl implements DSLYes {

}

@CompileStatic
interface MapPropertyTest {
    @DSLProperty(factory = { factory.newInstance(DSLYesImpl) })
    abstract MapProperty<String, DSLYes> getThings()

    @Inject
    abstract ObjectFactory getFactory()
}

@CompileStatic
interface FileTests {
    @ProjectGetter
    abstract Project getProject()

    @DSLProperty
    abstract RegularFileProperty getOutput()
    @DSLProperty
    abstract ConfigurableFileCollection getInputs()
    @DSLProperty
    abstract DirectoryProperty getDirectoryInput()
}