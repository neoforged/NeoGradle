import net.minecraftforge.gradle.dsl.specification.common.TestExtension

class Testing {
    fun test(ext: TestExtension): Unit {
        ext.helloWorld {
            it.thing = "12"
        }
        yes {

        }
    }

    fun yes(i: String.() -> Unit): Unit {
        i("12")
    }

    fun yesa(i: Function<String>): Unit {
    }
}