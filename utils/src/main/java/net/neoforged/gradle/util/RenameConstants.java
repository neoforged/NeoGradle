package net.neoforged.gradle.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;

/**
 * Constants used by the rename task
 */
public final class RenameConstants {

    /**
     * The default JVM arguments to use when running the rename task
     */
    public static final List<String> DEFAULT_JVM_ARGS = ImmutableList.of("-Xmx4g");

    /**
     * The default program arguments to use when running the rename task
     */
    public static final List<String> DEFAULT_PROGRAMM_ARGS = ImmutableList.<String>builder()
            .add(
                    "--input", "{input}",
                    "--output", "{output}",
                    "--names", "{mappings}",
                    "--cfg",
                    "{libraries}",
                    "--ann-fix", "--ids-fix", "--src-fix", "--record-fix"
            ).build();

    /**
     * The default task arguments to use when renaming the output
     */
    public static final ImmutableMap<String, String> DEFAULT_RENAME_VALUES = ImmutableMap.<String, String>builder()
            .put("libraries", "{librariesOutput}")
            .build();

    private RenameConstants() {
        throw new IllegalStateException("Can not instantiate an instance of: DecompileUtils. This is a utility class");
    }
}
