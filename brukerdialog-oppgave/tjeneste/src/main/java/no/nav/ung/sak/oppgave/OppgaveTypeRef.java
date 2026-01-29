package no.nav.ung.sak.oppgave;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Repeatable(OppgaveTypeRef.ContainerOfOppgaveTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface OppgaveTypeRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike oppgavetyper.
     * <p>
     * Må matche ett innslag i <code>OppgaveType</code> tabell for å kunne kjøres.
     *
     * @see OppgaveType
     */
    OppgaveType value();

    /**
     * container for repeatable annotations.
     *
     * @see <a href="https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html">...</a>
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
    @Documented
    public @interface ContainerOfOppgaveTypeRef {
        OppgaveTypeRef[] value();
    }

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    public static class OppgaveTypeRefLiteral extends AnnotationLiteral<OppgaveTypeRef> implements OppgaveTypeRef {

        private OppgaveType navn;

        public OppgaveTypeRefLiteral() {
            this.navn = null;
        }

        public OppgaveTypeRefLiteral(OppgaveType oppgaveType) {
            this.navn = (oppgaveType);
        }

        @Override
        public OppgaveType value() {
            return navn;
        }

    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, OppgaveType oppgaveTypeKode) {
            return find(cls, (CDI<I>) CDI.current(), oppgaveTypeKode);
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Instance<I> instances, OppgaveType oppgaveTypeKode) {
            return find(null, instances, oppgaveTypeKode);
        }

        public static <I> List<Instance<I>> list(Class<I> cls, Instance<I> instances, OppgaveType oppgaveTypeKode) {
            Objects.requireNonNull(instances, "instances");

            final List<Instance<I>> resultat = new ArrayList<>();
            Consumer<OppgaveType> search = (OppgaveType s) -> {
                var inst = select(cls, instances, new OppgaveTypeRefLiteral(s));
                if (inst.isUnsatisfied()) {
                    return;
                }
                resultat.add(inst);
            };

            coalesce(oppgaveTypeKode, null).forEach(search);
            return List.copyOf(resultat);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, OppgaveType ytelseTypeKode) {
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(ytelseTypeKode, null)) {
                var inst = select(cls, instances, new OppgaveTypeRefLiteral(fagsakLiteral));
                if (inst.isResolvable()) {
                    return Optional.of(getInstance(inst));
                } else {
                    if (inst.isAmbiguous()) {

                        String className = cls != null ? cls.getName() : "null";
                        String instancesClassName = classNameFromInstance(
                            instances);
                        throw new IllegalStateException(
                            "Har flere matchende instanser for klasse={" + className + "}, fra instances klass={" + instancesClassName + "}, fagsakType={" + fagsakLiteral + "}");
                    }
                }
            }

            return Optional.empty();
        }

        private static <I> String classNameFromInstance(Instance<I> instances) {
            try {
                return instances.iterator().next().getClass().getName();
            } catch (RuntimeException e) {
                return "Ukjent";
            }
        }

        private static <I> Instance<I> select(Class<I> cls, Instance<I> instances, Annotation anno) {
            return cls != null ? instances.select(cls, anno) : instances.select(anno);
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
            }
            return i;
        }

        private static <T> List<T> coalesce(T... vals) {
            return Arrays.stream(vals).distinct().collect(Collectors.toList());
        }
    }
}
