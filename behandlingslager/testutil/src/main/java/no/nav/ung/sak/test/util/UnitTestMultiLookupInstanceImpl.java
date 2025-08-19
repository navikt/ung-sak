package no.nav.ung.sak.test.util;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class UnitTestMultiLookupInstanceImpl<T> implements Instance<T> {

    private final List<T> values;

    public UnitTestMultiLookupInstanceImpl(List<T> values) {
        this.values = values == null ? Collections.emptyList() : new ArrayList<>(values);
    }

    public UnitTestMultiLookupInstanceImpl(T... values) {
        this.values = List.of(values);
    }

    @Override
    public T get() {
        if (values.isEmpty()) {
            throw new IllegalStateException("No instances available");
        }
        if (values.size() > 1) {
            throw new IllegalStateException("Multiple instances found, use iterator() instead");
        }
        return values.getFirst();
    }

    //Metodene her kan implementeres etter behov

    @Override
    public Instance<T> select(Annotation... annotations) {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Instance<U> select(Class<U> subtype, Annotation... annotations) {
        List<U> filteredValues = values.stream()
                .filter(v -> subtype.isAssignableFrom(v.getClass()))
                .map(v -> (U) v)
                .collect(Collectors.toList());
        return new UnitTestMultiLookupInstanceImpl<>(filteredValues);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> subtype, Annotation... annotations) {
        return (Instance<U>) this;
    }

    @Override
    public boolean isUnsatisfied() {
        return values.isEmpty();
    }

    @Override
    public boolean isAmbiguous() {
        return values.size() > 1;
    }

    @Override
    public void destroy(T instance) {
    }

    @Override
    public Handle<T> getHandle() {
        return null;
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        return values.iterator();
    }
}
