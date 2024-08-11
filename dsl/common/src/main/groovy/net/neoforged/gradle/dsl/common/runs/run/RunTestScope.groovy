package net.neoforged.gradle.dsl.common.runs.run;

import groovy.transform.CompileStatic
import net.minecraftforge.gdi.BaseDSLElement;
import net.minecraftforge.gdi.ConfigurableDSLElement
import net.minecraftforge.gdi.annotations.DSLProperty
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity;

/**
 * Defines a runs test scope if the run is configured for tests
 * <p>
 *     Note of the scope options listed below here, only the first is picked by the IDE or the syncing mechanism.
 *     Some combinations might be possible, like class and method.
 * </p>
 */
@CompileStatic
interface RunTestScope extends BaseDSLElement<RunTestScope> {

    /**
     * @return The name of the package that houses the tests.
     */
    @DSLProperty
    @Input
    @Optional
    Property<String> getPackageName()

    /**
     * @return The directory which houses the tests.
     */
    @DSLProperty
    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    DirectoryProperty getDirectory()

    /**
     * @return The pattern to use to find the tests.
     */
    @DSLProperty
    @Input
    @Optional
    Property<String> getPattern()

    /**
     * @return The class name of the test.
     */
    @DSLProperty
    @Input
    @Optional
    Property<String> getClassName()

    /**
     * @return The method name of the test.
     */
    @DSLProperty
    @Input
    @Optional
    Property<String> getMethod()

    /**
     * @return The category of the test.
     */
    @DSLProperty
    @Input
    @Optional
    Property<String> getCategory()

}
