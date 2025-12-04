package net.neoforged.gradle.common.tasks;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class InjectInterfacesTaskTest {
    private byte[] generateSimpleClass(String name, String[] interfaces, String signature) {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, name, signature, "java/lang/Object", interfaces);
        cw.visitEnd();
        return cw.toByteArray();
    }

    private ClassNode visitWithInjection(byte[] classBytes, Map<String, List<String>> injectionMap) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        InjectInterfacesTask.InjectInterfacesClassVisitor visitor = new InjectInterfacesTask.InjectInterfacesClassVisitor(Opcodes.ASM9, cn, injectionMap);
        cr.accept(visitor, 0);
        return cn;
    }

    @Test
    void injectPlainInterface() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className, new String[]{}, null);
        Map<String, List<String>> map = Map.of(className, List.of("com/example/InjectedInterface"));
        ClassNode cn = visitWithInjection(original, map);
        assertTrue(cn.interfaces.contains("com/example/InjectedInterface"));
        assertNotNull(cn.signature);
    }

    @Test
    void injectInnerInterface() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className, new String[]{}, null);
        Map<String, List<String>> map = Map.of(className, List.of("com/example/Outer$InnerInterface"));
        ClassNode cn = visitWithInjection(original, map);
        assertTrue(cn.interfaces.contains("com/example/Outer$InnerInterface"));
    }

    @Test
    void injectGenericInterfaceWithTypeParam() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className, new String[]{}, "<T:Ljava/lang/Object;>Ljava/lang/Object;" );
        Map<String, List<String>> map = Map.of(className, List.of("java/util/function/Supplier<T>"));
        ClassNode cn = visitWithInjection(original, map);
        assertTrue(cn.interfaces.contains("java/util/function/Supplier"));
        assertNotNull(cn.signature);
        assertTrue(cn.signature.contains("Ljava/util/function/Supplier<TT;>;"));
    }

    @Test
    void injectGenericInterfaceWithClassParam() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className, new String[]{}, null);
        Map<String, List<String>> map = Map.of(className, List.of("java/util/function/Supplier<java/util/List>"));
        ClassNode cn = visitWithInjection(original, map);
        assertTrue(cn.interfaces.contains("java/util/function/Supplier"));
        assertTrue(cn.signature.contains("Ljava/util/function/Supplier<Ljava/util/List;>;"));
    }

    @Test
    void injectMultipleInterfaces() {
        String className = "com/example/TestClass";
        byte[] original = generateSimpleClass(className, new String[]{}, null);
        Map<String, List<String>> map = Map.of(className, List.of("com/example/One", "com/example/Two<T>", "com/example/Three<java/util/List>"));
        ClassNode cn = visitWithInjection(original, map);
        assertTrue(cn.interfaces.contains("com/example/One"));
        assertTrue(cn.interfaces.contains("com/example/Two"));
        assertTrue(cn.interfaces.contains("com/example/Three"));
        assertTrue(cn.signature.contains("Lcom/example/Two<TT;>;"));
        assertTrue(cn.signature.contains("Lcom/example/Three<Ljava/util/List;>;"));
    }
}

