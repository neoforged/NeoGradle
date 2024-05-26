package net.neoforged.gradle.common.util;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.DomainObjectCollection;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectCollectionSchema;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.Rule;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.provider.Provider;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DelegatingDomainObjectContainer<T> implements NamedDomainObjectContainer<T> {

    private final NamedDomainObjectContainer<T> delegate;

    @Inject
    public DelegatingDomainObjectContainer(NamedDomainObjectContainer<T> delegate) {
        this.delegate = delegate;
    }

    @Inject
    public DelegatingDomainObjectContainer(final Project project, final Class<T> valuesClass, final NamedDomainObjectFactory<T> factory) {
        this.delegate = project.container(valuesClass, factory);
    }

    @Override
    public T create(String name) throws InvalidUserDataException {
        return delegate.create(name);
    }

    @Override
    public T maybeCreate(String name) {
        return delegate.maybeCreate(name);
    }

    @Override
    public T create(String name, Closure configureClosure) throws InvalidUserDataException {
        return delegate.create(name, configureClosure);
    }

    @Override
    public T create(String name, Action<? super T> configureAction) throws InvalidUserDataException {
        return delegate.create(name, configureAction);
    }

    @Override
    public NamedDomainObjectContainer<T> configure(Closure configureClosure) {
        return delegate.configure(configureClosure);
    }

    @Override
    public NamedDomainObjectProvider<T> register(String name, Action<? super T> configurationAction) throws InvalidUserDataException {
        return delegate.register(name, configurationAction);
    }

    @Override
    public NamedDomainObjectProvider<T> register(String name) throws InvalidUserDataException {
        return delegate.register(name);
    }

    @Override
    public <S extends T> NamedDomainObjectSet<S> withType(Class<S> type) {
        return delegate.withType(type);
    }


    @Override
    public NamedDomainObjectSet<T> named(Spec<String> nameFilter) {
        return delegate.named(nameFilter);
    }

    @Override
    public NamedDomainObjectSet<T> matching(Spec<? super T> spec) {
        return delegate.matching(spec);
    }

    @Override
    public NamedDomainObjectSet<T> matching(Closure spec) {
        return delegate.matching(spec);
    }

    @Override
    public Set<T> findAll(Closure spec) {
        return delegate.findAll(spec);
    }

    @Override
    public boolean add(T e) {
        return delegate.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return delegate.addAll(c);
    }

    @Override
    public Namer<T> getNamer() {
        return delegate.getNamer();
    }

    @Override
    public SortedMap<String, T> getAsMap() {
        return delegate.getAsMap();
    }

    @Override
    public SortedSet<String> getNames() {
        return delegate.getNames();
    }

    @Override
    @Nullable
    public T findByName(String name) {
        return delegate.findByName(name);
    }

    @Override
    public T getByName(String name) throws UnknownDomainObjectException {
        return delegate.getByName(name);
    }

    @Override
    public T getByName(String name, Closure configureClosure) throws UnknownDomainObjectException {
        return delegate.getByName(name, configureClosure);
    }

    @Override
    public T getByName(String name, Action<? super T> configureAction) throws UnknownDomainObjectException {
        return delegate.getByName(name, configureAction);
    }

    @Override
    public T getAt(String name) throws UnknownDomainObjectException {
        return delegate.getAt(name);
    }

    @Override
    public Rule addRule(Rule rule) {
        return delegate.addRule(rule);
    }

    @Override
    public Rule addRule(String description, Closure ruleAction) {
        return delegate.addRule(description, ruleAction);
    }

    @Override
    public Rule addRule(String description, Action<String> ruleAction) {
        return delegate.addRule(description, ruleAction);
    }

    @Override
    public List<Rule> getRules() {
        return delegate.getRules();
    }

    @Override
    public NamedDomainObjectProvider<T> named(String name) throws UnknownDomainObjectException {
        return delegate.named(name);
    }

    @Override
    public NamedDomainObjectProvider<T> named(String name, Action<? super T> configurationAction) throws UnknownDomainObjectException {
        return delegate.named(name, configurationAction);
    }

    @Override
    public <S extends T> NamedDomainObjectProvider<S> named(String name, Class<S> type) throws UnknownDomainObjectException {
        return delegate.named(name, type);
    }

    @Override
    public <S extends T> NamedDomainObjectProvider<S> named(String name, Class<S> type, Action<? super S> configurationAction) throws UnknownDomainObjectException {
        return delegate.named(name, type, configurationAction);
    }

    @Override
    @Internal
    public NamedDomainObjectCollectionSchema getCollectionSchema() {
        return delegate.getCollectionSchema();
    }

    @Override
    public void addLater(Provider<? extends T> provider) {
        delegate.addLater(provider);
    }

    @Override
    public void addAllLater(Provider<? extends Iterable<T>> provider) {
        delegate.addAllLater(provider);
    }

    @Override
    public <S extends T> DomainObjectCollection<S> withType(Class<S> type, Action<? super S> configureAction) {
        return delegate.withType(type, configureAction);
    }

    @Override
    public <S extends T> DomainObjectCollection<S> withType(Class<S> type, Closure configureClosure) {
        return delegate.withType(type, configureClosure);
    }

    @Override
    public Action<? super T> whenObjectAdded(Action<? super T> action) {
        return delegate.whenObjectAdded(action);
    }

    @Override
    public void whenObjectAdded(Closure action) {
        delegate.whenObjectAdded(action);
    }

    @Override
    public Action<? super T> whenObjectRemoved(Action<? super T> action) {
        return delegate.whenObjectRemoved(action);
    }

    @Override
    public void whenObjectRemoved(Closure action) {
        delegate.whenObjectRemoved(action);
    }

    @Override
    public void all(Action<? super T> action) {
        delegate.all(action);
    }

    @Override
    public void all(Closure action) {
        delegate.all(action);
    }

    @Override
    public void configureEach(Action<? super T> action) {
        delegate.configureEach(action);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return delegate.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public Stream<T> stream() {
        return delegate.stream();
    }

    @Override
    public Stream<T> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        delegate.forEach(action);
    }


}
