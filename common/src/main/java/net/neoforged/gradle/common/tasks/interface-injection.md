### Interface Injection
JST supports injecting interfaces to transformed classes in a data-driven fashion, and creates **empty** stubs for the classes
to be able to still compile the modified code without access to the actual interface definitions.  
This feature allows interfaces added at runtime using Mixins, Coremods, or other transformation mechanics to be visible at compile-time.

The format of the interface injection data file is quite straightforward:
```json5
{
  "targetClassBinary": ["interfaceBinary"] // Can be a single interface, or an array of interfaces to implement
}
```
Where:
- `targetClassBinary` is the binary representation of the target class to inject the interfaces to (e.g. `net/minecraft/world/item/Item`, or `net/minecraft/world/item/Item$Properties`)
- `interfaceBinary` is the binary representation of an interface to inject (e.g. `com/example/examplemod/MyInjectedInterface`)

The interfaces can have generic parameter declarations. Assuming the target class `EntityType` has a `T` generic parameter,
we could implement `Supplier<T>` using `java/util/function/Supplier<T>`.

> [!NOTE]  
> Generics are *copied verbatim*. If you need the generics to reference a class, please use its fully qualified name (e.g. `java/util/function/Supplier<java.util.concurrent.atomic.AtomicInteger>`). As an exception to this rule, inner classes should be separated by `$` (e.g. `java/util/function/Supplier<java.util.Map$Entry>`).