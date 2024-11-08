package no.nav.k9.sak.test.util;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

public class UnitTestLookupInstanceImpl<T> implements Instance<T> {

    T verdi;

    public UnitTestLookupInstanceImpl(T verdi) {
        this.verdi = verdi;
    }

    @Override
    public T get() {
        return verdi;
    }

    //Metodene her kan implementeres etter behov

    @Override
    public Instance<T> select(Annotation... annotations) {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Instance<U> select(Class<U> aClass, Annotation... annotations) {
        return (Instance<U>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U extends T> Instance<U> select(TypeLiteral<U> typeLiteral, Annotation... annotations) {
        return (Instance<U>) this;
    }

    @Override
    public boolean isUnsatisfied() {
        return false;
    }

    @Override
    public boolean isAmbiguous() {
        return false;
    }

    @Override
    public void destroy(T t) {
    }

    @Override
    public Handle<T> getHandle() {
        return null;
    }

    @Override
    public Iterable<? extends Handle<T>> handles() {
        return Set.of();
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(get()).iterator();
    }
}
