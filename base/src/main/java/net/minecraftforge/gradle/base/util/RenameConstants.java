package net.minecraftforge.gradle.base.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

public final class RenameConstants {

    public static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx4g");
    public static final List<String> DEFAULT_PROGRAMM_ARGS = ImmutableList.<String>builder()
            .add(
                    "--input",
                    "{input}",
                    "--output",
                    "{output}",
                    "--map",
                    "{mappings}",
                    "--cfg",
                    "{libraries}",
                    "--ann-fix",
                    "--ids-fix",
                    "--src-fix",
                    "--record-fix",
                    "--reverse"
            ).build();

    public static final ImmutableMap<String, String> DEFAULT_RENAME_VALUES = ImmutableMap.<String, String>builder()
            .put("libraries", "{librariesOutput}")
            .build();

    private RenameConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: DecompileUtils. This is a utility class");
    }
}
