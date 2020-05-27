package no.nav.k9.sak.domene.registerinnhenting;

import java.lang.annotation.Annotation;
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


    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        private static String getName(Class<?> aggregat) {
            String aggrNavn = aggregat.isAnnotationPresent(Entity.class) ? aggregat.getAnnotation(Entity.class).name()  : aggregat.getSimpleName();
            return aggrNavn;
        }

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode, eller default '*' implementasjon. Merk at Instance bør være
         * injected med riktig forventet klassetype og @Any qualifier.
         */
        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType fagsakYtelseType, Instance<I> instances, Class<?> aggregatClass) {
            return find(cls, instances, getName(aggregatClass), fagsakYtelseType.getKode());
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String aggregatClassName, String fagsakYtelseType) {
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(fagsakYtelseType, "*")) {
                var inst = select(cls, instances, new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakLiteral));
                if (inst.isUnsatisfied()) {
                    continue;
                } else {
                    for (var grunnlagLiteral : coalesce(aggregatClassName, "*")) {
                        var ginst = select(cls, inst, new GrunnlagRefLiteral(grunnlagLiteral));
                        if (ginst.isResolvable()) {
                            return Optional.of(getInstance(ginst));
                        } else {
                            if (ginst.isAmbiguous()) {
                                throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() +
                                    ", aggegatKlasse=" + aggregatClassName + ", fagsakType=" + fagsakYtelseType);
                            }
                        }
                    }
                }
            }

            return Optional.empty();
        }

        private static <I> Instance<I> select(Class<I> cls, Instance<I> instances, Annotation anno) {
            return cls != null
                ? instances.select(cls, anno)
                : instances.select(anno);
        }

        private static <I> I getInstance(Instance<I> inst) {
            var i = inst.get();
            if (i.getClass().isAnnotationPresent(Dependent.class)) {
                throw new IllegalStateException(
                    "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + i.getClass());
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
