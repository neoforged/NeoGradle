package net.minecraftforge.gradle.dsl.annotations

import org.codehaus.groovy.transform.GroovyASTTransformationClass
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty

import java.lang.annotation.*

/**
 * Annotate an <strong>abstract method of a groovy interface</strong> in order to generate DSL methods for the property. <br>
 * This annotation will generate the following methods based on the return type of the method:
 * <table>
 *     <tr>
 *          <th>Property Type</th>
 *          <th>Generated Methods</th>
 *     </tr>
 *     <tr>
 *          <th>{@link org.gradle.api.provider.Property}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(T)}</li>
 *                  <li>{@code $propertyName(Action<T>)} - the action will be executed on the provider's value, and if it doesn't have a value, one will be created using the {@linkplain DSLProperty#factory() factory} </li>
 *                  <li>{@code $propertyName(@DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *                  <li>{@code $propertyName(T, Action<T>)} - the action will be executed on the given value, which will be then {@linkplain org.gradle.api.provider.Property#set(java.lang.Object) set} as the provider's value</li>
 *                  <li>{@code $propertyName(T, @DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>any {@link org.gradle.api.provider.HasMultipleValues} ({@link ListProperty}, {@link SetProperty})</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(T)} - the given value will be added to the list</li>
 *                  <li>{@code $propertyName(T, Action<T>)} - the action will be executed on the given value, which will be then added to the list</li>
 *                  <li>{@code $propertyName(T, @DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *              The methods below are generated <i>only</i> if a {@linkplain DSLProperty#factory() factory} is supplied:
 *              <ul>
 *                  <li>{@code $propertyName(Action<T>)} - the action will be executed on an object created with the factory, which will be then added to the list</li>
 *                  <li>{@code $propertyName(@DelegatesTo(T.class) Closure<T>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link org.gradle.api.provider.MapProperty}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(K, V)} - the given value will be added to the map, at the given key</li>
 *                  <li>{@code $propertyName(K, V, Action<V>)} - the action will be executed on the given value, which will be then added to the map at the given key</li>
 *                  <li>{@code $propertyName(K, V, @DelegatesTo(V.class) Closure<V>)} - same behaviour as the action</li>
 *              </ul>
 *              The methods below are generated <i>only</i> if a {@linkplain DSLProperty#factory() factory} is supplied:
 *              <ul>
 *                  <li>{@code $propertyName(K, Action<V>)} - the action will be executed on an object created with the factory, which will be then added to the map at the given key</li>
 *                  <li>{@code $propertyName(K, @DelegatesTo(V.class) Closure<V>)} - same behaviour as the action</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link NamedDomainObjectContainer}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(String, Action<T>)} - calls {@link NamedDomainObjectContainer#register(java.lang.String, Action)}</li>
 *                  <li>{@code $propertyName(K, @DelegatesTo(V.class) Closure<V>)} - calls {@link NamedDomainObjectContainer#register(java.lang.String, Action)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <br>
 *     <tr>
 *          <th>{@link DirectoryProperty} - requires a {@link ProjectGetter}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(Directory)} - calls {@link DirectoryProperty#value(Directory)}</li>
 *                  <li>{@code $propertyName(File)} - calls {@link DirectoryProperty#set(java.io.File)}</li>
 *                  <li>{@code $propertyName(Object)} - calls {@link DirectoryProperty#set(java.io.File)} with a file got using {@link Project#file(java.lang.Object)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link RegularFileProperty} - requires a {@link ProjectGetter}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(RegularFile)} - calls {@link RegularFileProperty#value(RegularFile)}</li>
 *                  <li>{@code $propertyName(File)} - calls {@link RegularFileProperty#set(java.io.File)}</li>
 *                  <li>{@code $propertyName(Object)} - calls {@link RegularFileProperty#set(java.io.File)} with a file got using {@link Project#file(java.lang.Object)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 *     <tr>
 *          <th>{@link ConfigurableFileCollection}</th>
 *          <th>
 *              <ul>
 *                  <li>{@code $propertyName(Object)} - calls {@link ConfigurableFileCollection#from(java.lang.Object...)}</li>
 *                  <li>{@code $propertyName(Object...)} - calls {@link ConfigurableFileCollection#from(java.lang.Object...)}</li>
 *              </ul>
 *          </th>
 *     </tr>
 * </table>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@GroovyASTTransformationClass('net.minecraftforge.gradle.dsl.generator.transform.DSLPropertyTransformer')
@interface DSLProperty {
    String propertyName() default ''

    Class<Closure> factory() default Closure.class

    boolean isConfigurable() default true
}
