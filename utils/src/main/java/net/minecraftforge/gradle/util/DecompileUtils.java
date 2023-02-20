package net.minecraftforge.gradle.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * A utility class for decompilation invocation of ForgeFlower.
 */
public final class DecompileUtils {

    /**
     * The default JVM arguments for the decompiler.
     */
    public static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx4g");

    /**
     * The default program arguments for the decompiler.
     */
    public static final List<String> DEFAULT_PROGRAMM_ARGS = ImmutableList.<String>builder()
            .add(
                    "-din=1",
                    "-rbr=1",
                    "-dgs=1",
                    "-asc=1",
                    "-rsy=1",
                    "-iec=1",
                    "-jvn=1",
                    "-isl=0",
                    "-iib=1",
                    "-bsm=1",
                    "-dcl=1",
                    "-log=TRACE",
                    "-cfg",
                    "{libraries}",
                    "{input}",
                    "{output}"
            ).build();

    /**
     * The default values for the decompiler.
     */
    public static final ImmutableMap<String, String> DEFAULT_DECOMPILE_VALUES = ImmutableMap.<String, String>builder()
            .put("libraries", "{librariesOutput}")
            .put("input", "{renameOutput}")
            .build();

    private DecompileUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: DecompileUtils. This is a utility class");
    }
}
