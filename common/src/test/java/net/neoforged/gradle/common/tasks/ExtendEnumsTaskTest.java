package net.neoforged.gradle.common.tasks;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExtendEnumsTaskTest {
    private byte[] generateSimpleClass(String name) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, name, null, "java/lang/Enum", null);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private ClassNode visitWithExtension(byte[] classBytes, Map<String, Set<String>> injectionMap) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        ExtendEnumsTask.ExtendEnumsClassVisitor visitor = new ExtendEnumsTask.ExtendEnumsClassVisitor(Opcodes.ASM9, cn, injectionMap);
        cr.accept(visitor, 0);
        return cn;
    }

    @Test
    void injectMultipleEnumEntries() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className);
        Map<String, Set<String>> map = Map.of(className, Set.of("A", "B"));
        ClassNode cn = visitWithExtension(original, map);
        assertEquals(2, cn.fields.size()); // In this case we never generated the backing values field
        assertEquals("A", cn.fields.get(0).name);
        assertEquals("B", cn.fields.get(1).name);
        for (var field : cn.fields) {
            assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, field.access);
            assertNotNull(field.invisibleAnnotations);
            assertEquals(1, field.invisibleAnnotations.size());
            assertEquals("Lnet/neoforged/fml/common/asm/enumextension/ExtensionEnumEntry;", field.invisibleAnnotations.get(0).desc);
        }
    }
}

