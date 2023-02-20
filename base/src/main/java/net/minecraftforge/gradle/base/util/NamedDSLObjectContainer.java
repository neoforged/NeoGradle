package net.minecraftforge.gradle.base.util;

import groovy.lang.Closure;
import net.minecraftforge.gradle.dsl.base.BaseDSLElement;
import net.minecraftforge.gradle.dsl.base.util.NamedDSLElement;
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
import org.gradle.util.ConfigureUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

 public abstract class NamedDSLObjectContainer<TEntry extends BaseDSLElement<TEntry> & NamedDSLElement> implements NamedDomainObjectContainer<TEntry> {

    private final Project project;
    private final NamedDomainObjectContainer<TEntry> delegate;

    @Inject
    public NamedDSLObjectContainer(final Project project, final Class<TEntry> valueType, final NamedDomainObjectFactory<TEntry> factory) {
        this.delegate = project.getObjects().domainObjectContainer(valueType, factory);
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    @Override
    public TEntry create(String name) throws InvalidUserDataException {
        return this.delegate.create(name);
    }

    @Override
    public TEntry maybeCreate(String name) {
        return this.delegate.maybeCreate(name);
    }

    @Override
    public TEntry create(String name, Action<? super TEntry> configureAction) throws InvalidUserDataException {
        return this.delegate.create(name, configureAction);
    }

    @Override
    public TEntry create(String name, Closure configurator) throws InvalidUserDataException {
        return this.delegate.create(name, configurator);
    }

    @Override
    public NamedDomainObjectProvider<TEntry> register(String name, Action<? super TEntry> configurationAction) throws InvalidUserDataException {
        return this.delegate.register(name, configurationAction);
    }

    @Override
    public NamedDomainObjectProvider<TEntry> register(String name) throws InvalidUserDataException {
        return this.delegate.register(name);
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return this.delegate.contains(o);
    }

    @NotNull
    @Override
    public Iterator<TEntry> iterator() {
        return this.delegate.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return this.delegate.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return this.delegate.toArray(a);
    }

    @Override
    public boolean add(TEntry e) {
        return this.delegate.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return this.delegate.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return this.delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends TEntry> c) {
        return this.delegate.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return this.delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return this.delegate.retainAll(c);
    }

    @Override
    public void clear() {
        this.delegate.clear();
    }

    @Override
    public Namer<TEntry> getNamer() {
        return this.delegate.getNamer();
    }

    @Override
    public SortedMap<String, TEntry> getAsMap() {
        return this.delegate.getAsMap();
    }

    @Override
    public SortedSet<String> getNames() {
        return this.delegate.getNames();
    }

    @Nullable
    @Override
    public TEntry findByName(String name) {
        return this.delegate.findByName(name);
    }

    @Override
    public TEntry getByName(String name) throws UnknownDomainObjectException {
        return this.delegate.getByName(name);
    }

    @Override
    public TEntry getByName(String name, Closure configureClosure) throws UnknownDomainObjectException {
        return this.delegate.getByName(name, configureClosure);
    }

    @Override
    public TEntry getByName(String name, Action<? super TEntry> configureAction) throws UnknownDomainObjectException {
        return this.delegate.getByName(name, configureAction);
    }

    @Override
    public TEntry getAt(String name) throws UnknownDomainObjectException {
        return this.delegate.getAt(name);
    }

    @Override
    public Rule addRule(Rule rule) {
        return this.delegate.addRule(rule);
    }

    @Override
    public Rule addRule(String description, Closure ruleAction) {
        return this.delegate.addRule(description, ruleAction);
    }

    @Override
    public Rule addRule(String description, Action<String> ruleAction) {
        return this.delegate.addRule(description, ruleAction);
    }

    @Override
    public List<Rule> getRules() {
        return this.delegate.getRules();
    }

    @Override
    public void addLater(Provider<? extends TEntry> provider) {
        this.delegate.addLater(provider);
    }

    @Override
    public void addAllLater(Provider<? extends Iterable<TEntry>> provider) {
        this.delegate.addAllLater(provider);
    }

    @Override
    public <S extends TEntry> NamedDomainObjectSet<S> withType(Class<S> type) {
        return this.delegate.withType(type);
    }

    @Override
    public <S extends TEntry> DomainObjectCollection<S> withType(Class<S> type, Action<? super S> configureAction) {
        return this.delegate.withType(type, configureAction);
    }

    @Override
    public <S extends TEntry> DomainObjectCollection<S> withType(Class<S> type, Closure configureClosure) {
        return this.delegate.withType(type, configureClosure);
    }

    @Override
    public NamedDomainObjectSet<TEntry> matching(Spec<? super TEntry> spec) {
        return this.delegate.matching(spec);
    }

    @Override
    public NamedDomainObjectSet<TEntry> matching(Closure spec) {
        return this.delegate.matching(spec);
    }

    @Override
    public Action<? super TEntry> whenObjectAdded(Action<? super TEntry> action) {
        return this.delegate.whenObjectAdded(action);
    }

    @Override
    public void whenObjectAdded(Closure action) {
        this.delegate.whenObjectAdded(action);
    }

    @Override
    public Action<? super TEntry> whenObjectRemoved(Action<? super TEntry> action) {
        return this.delegate.whenObjectRemoved(action);
    }

    @Override
    public void whenObjectRemoved(Closure action) {
        this.delegate.whenObjectRemoved(action);
    }

    @Override
    public void all(Action<? super TEntry> action) {
        this.delegate.all(action);
    }

    @Override
    public void all(Closure action) {
        this.delegate.all(action);
    }

    @Override
    public void configureEach(Action<? super TEntry> action) {
        this.delegate.configureEach(action);
    }

    @Override
    public NamedDomainObjectProvider<TEntry> named(String name) throws UnknownDomainObjectException {
        return this.delegate.named(name);
    }

    @Override
    public NamedDomainObjectProvider<TEntry> named(String name, Action<? super TEntry> configurationAction) throws UnknownDomainObjectException {
        return this.delegate.named(name, configurationAction);
    }

    @Override
    public <S extends TEntry> NamedDomainObjectProvider<S> named(String name, Class<S> type) throws UnknownDomainObjectException {
        return this.delegate.named(name, type);
    }

    @Override
    public <S extends TEntry> NamedDomainObjectProvider<S> named(String name, Class<S> type, Action<? super S> configurationAction) throws UnknownDomainObjectException {
        return this.delegate.named(name, type, configurationAction);
    }

    @Override
    public NamedDomainObjectCollectionSchema getCollectionSchema() {
        return this.delegate.getCollectionSchema();
    }

    @Override
    public Set<TEntry> findAll(Closure spec) {
        return this.delegate.findAll(spec);
    }

     @SuppressWarnings("deprecation")
     @Override
     public NamedDSLObjectContainer<TEntry> configure(Closure configureClosure) {
         return ConfigureUtil.configureSelf(configureClosure, this);
     }

     public Object methodMissing(String name, Object args) {
        Object[] params = (Object[])args;
        try {
            if (params.length == 0) {
                return this.delegate.getByName(name);
            } else if (params.length == 1 && params[0] instanceof Closure) {
                return this.delegate.getByName(name, (Closure)params[0]);
            } else if (params.length == 1 && params[0] instanceof Action) {
                return this.delegate.getByName(name, (Action) params[0]);
            } else {
                throw new IllegalStateException("Cannot invoke method " + name + " with arguments " + Arrays.toString(params));
            }
        } catch (UnknownDomainObjectException var5) {
            if (params.length == 0) {
                return this.delegate.create(name);
            } else if (params.length == 1 && params[0] instanceof Closure) {
                return this.delegate.create(name, (Closure)params[0]);
            } else if (params.length == 1 && params[0] instanceof Action) {
                return this.delegate.create(name, (Action) params[0]);
            } else {
                throw new IllegalStateException("Cannot invoke method " + name + " with arguments " + Arrays.toString(params));
            }
        }

    }
}
