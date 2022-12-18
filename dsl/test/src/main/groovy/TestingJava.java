import net.minecraftforge.gradle.dsl.specification.common.TestExtension;

public class TestingJava {
    public static void yes(TestExtension extension) {
        extension.helloWorld(helloWorld -> helloWorld.setThing("thingy"));
    }
}
