import groovy.transform.CompileStatic
import net.minecraftforge.gradle.dsl.specification.common.TestExtension

@CompileStatic
class Tesing {
    void yes(TestExtension ext) {
        ext.helloWorld {
            thing = '12'
        }
        ext.helloWorld(new TestExtension.HelloWorld())

        ext.listObject(new TestExtension.ListObject())
    }
}
