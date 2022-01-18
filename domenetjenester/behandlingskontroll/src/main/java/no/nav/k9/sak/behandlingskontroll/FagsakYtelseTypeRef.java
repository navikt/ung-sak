package no.nav.k9.sak.behandlingskontroll;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Stereotype;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef.ContainerOfFagsakYtelseTypeRef;

/**
 * Marker type som implementerer interface {@link BehandlingSteg} for å skille ulike implementasjoner av samme steg for ulike ytelser (eks.
 * Foreldrepenger vs. Engangsstønad).<br>
 * <p>
 * NB: Settes kun dersom det er flere implementasjoner med samme {@link BehandlingStegRef}.
 */
@Repeatable(ContainerOfFagsakYtelseTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface FagsakYtelseTypeRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandling typer.
     * <p>
     * Må matche ett innslag i <code>FAGSAK_YTELSE_TYPE</code> tabell for å kunne kjøres.
     *
     * @see no.nav.k9.kodeverk.behandling.FagsakYtelseType
     */
    String value()

    default "*";

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
    @Documented
    public @interface ContainerOfFagsakYtelseTypeRef {
        FagsakYtelseTypeRef[] value();
    }

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    public static class FagsakYtelseTypeRefLiteral extends AnnotationLiteral<FagsakYtelseTypeRef> implements FagsakYtelseTypeRef {

        private String navn;

        public FagsakYtelseTypeRefLiteral() {
            this("*");
        }

        public FagsakYtelseTypeRefLiteral(String navn) {
            this.navn = navn;
        }

        public FagsakYtelseTypeRefLiteral(FagsakYtelseType ytelseType) {
            this.navn = (ytelseType == null ? "*" : ytelseType.getKode());
        }

        @Override
        public String value() {
            return navn;
        }

    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, String ytelseTypeKode) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode);
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode.getKode());
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseTypeKode) {
            return find(cls, instances, ytelseTypeKode.getKode());
        }

        public static <I> Optional<I> find(Instance<I> instances, String ytelseTypeKode) {
            return find(null, instances, ytelseTypeKode);
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Instance<I> instances, FagsakYtelseType ytelseTypeKode) {
            return find(null, instances, ytelseTypeKode.getKode());
        }

        public static <I> List<Instance<I>> list(Class<I> cls, Instance<I> instances, String ytelseTypeKode) {
            Objects.requireNonNull(instances, "instances");

            final List<Instance<I>> resultat = new ArrayList<>();
            Consumer<String> search = (String s) -> {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(s));
                if (inst.isUnsatisfied()) {
                    return;
                }
                resultat.add(inst);
            };

            search.accept(ytelseTypeKode);
            search.accept("*"); // finn default
            return List.copyOf(resultat);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String ytelseTypeKode) {
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(ytelseTypeKode, "*")) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));
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

        private static List<String> coalesce(String... vals) {
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        }
    }
}
