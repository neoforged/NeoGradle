package net.minecraftforge.gradle.common.util;

import com.google.common.collect.ImmutableList;

import java.util.List;

public final class DecompileUtils {

    public static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx4g");
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

    private DecompileUtils() {
        throw new IllegalStateException("Can not instantiate an instance of: DecompileUtils. This is a utility class");
    }
}
