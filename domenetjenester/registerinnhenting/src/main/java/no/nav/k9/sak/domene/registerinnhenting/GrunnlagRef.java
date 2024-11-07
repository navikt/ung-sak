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

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.persistence.Entity;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Marker type som implementerer interface {@link EndringStartpunktUtleder}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface GrunnlagRef {

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen.
     * @return
     */
    Class<?> value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class GrunnlagRefLiteral extends AnnotationLiteral<GrunnlagRef> implements GrunnlagRef {

        private Class<?> navn;

        public GrunnlagRefLiteral(Class<?> navn) {
            this.navn = navn != null ? navn : GrunnlagRefLiteral.class;
        }

        @Override
        public Class<?> value() {
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
        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, Class<?> aggrNavn, FagsakYtelseType ytelseType) {
            Objects.requireNonNull(instances);
            Objects.requireNonNull(aggrNavn);
            Objects.requireNonNull(ytelseType);

            var fagsakInstances = FagsakYtelseTypeRef.Lookup.list(cls, instances, ytelseType);

            for (var inst : fagsakInstances) {
                for (var navn : coalesce(aggrNavn, GrunnlagRefLiteral.class)) {

                    Instance<I> selected = inst.select(new GrunnlagRef.GrunnlagRefLiteral(navn));
                    if (selected.isUnsatisfied()) {
                        continue; // matchet ikke
                    } else if (selected.isResolvable()) {
                        return Optional.of(getInstance(selected));
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

        @SafeVarargs
        private static <T> List<T> coalesce(T... vals) {
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().collect(Collectors.toList());
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
