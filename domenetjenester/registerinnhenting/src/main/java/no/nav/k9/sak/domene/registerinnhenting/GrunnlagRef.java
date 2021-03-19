package no.nav.k9.sak.domene.registerinnhenting;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.persistence.Entity;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Marker type som implementerer interface {@link StartpunktUtleder}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface GrunnlagRef {

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen.
     */
    String value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class GrunnlagRefLiteral extends AnnotationLiteral<GrunnlagRef> implements GrunnlagRef {

        private String navn;

        public GrunnlagRefLiteral(String navn) {
            this.navn = navn;
        }

        @Override
        public String value() {
            return navn;
        }
    }

    final class Lookup {

        private Lookup() {
        }

        private static String getName(Class<?> aggregat) {
            String aggrNavn = aggregat.isAnnotationPresent(Entity.class) ? aggregat.getAnnotation(Entity.class).name() : aggregat.getSimpleName();
            return aggrNavn;
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, Class<?> aggregatClass, FagsakYtelseType ytelseType) {
            return find(cls, instances, getName(aggregatClass), ytelseType);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String aggrNavn, FagsakYtelseType ytelseType) {
            Objects.requireNonNull(instances);
            Objects.requireNonNull(aggrNavn);
            Objects.requireNonNull(ytelseType);

            var fagsakInstances = FagsakYtelseTypeRef.Lookup.list(cls, instances, ytelseType.getKode());

            for (var inst : fagsakInstances) {
                for (var navn : coalesce(aggrNavn, "*")) {

                    Instance<I> selected = inst.select(new GrunnlagRef.GrunnlagRefLiteral(navn));
                    if (selected.isUnsatisfied()) {
                        continue; // matchet ikke
                    } else if (selected.isResolvable()) {
                        return Optional.of(getInstance(inst));
                    } else if (selected.isAmbiguous()) {
                        throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", ytelseType=" + ytelseType);
                    }

                }
            }

            return Optional.empty();
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
            }
            return i;
        }

        private static List<String> coalesce(String... vals) {
            return Arrays.asList(vals).stream().filter(v -> v != null).distinct().collect(Collectors.toList());
        }
    }

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD })
    @Documented
    public @interface ContainerOfGrunnlagRef {
        GrunnlagRef[] value();
    }
}
