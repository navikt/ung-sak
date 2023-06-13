package no.nav.k9.sak.behandling.revurdering.etterkontroll;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
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
import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste.KontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;

/**
 * Marker type som implementerer interface {@link KontrollTjeneste} for å skille ulike implementasjoner av samme kontrolltjenese for ulike
 * kontroll typer.<br>
 * <p>
 * NB: Settes kun dersom det er flere implementasjoner av med samme {@link KontrollTjeneste}.
 */
@Repeatable(EtterkontrollRef.ContainerOfEtterkontrollRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Documented
public @interface EtterkontrollRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike kontroll typer.
     * <p>
     * Må matche ett innslag i <code>KontrollType</code> tabell for å kunne kjøres.
     *
     * @see KontrollType
     */
    KontrollType value() default KontrollType.UDEFINERT;

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER})
    @Documented
    public @interface ContainerOfEtterkontrollRef {
        EtterkontrollRef[] value();
    }

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    public static class EtterkontrollRefLiteral extends AnnotationLiteral<EtterkontrollRef> implements EtterkontrollRef {

        private KontrollType type;

        public EtterkontrollRefLiteral() {
            this.type = KontrollType.UDEFINERT;
        }

        public EtterkontrollRefLiteral(KontrollType type) {
            this.type = (type == null ? KontrollType.UDEFINERT : type);
        }

        @Override
        public KontrollType value() {
            return type;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Instance<I> instances, FagsakYtelseType ytelseTypeKode, KontrollType startpunktRef) {
            return find(null, instances, ytelseTypeKode, startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode, KontrollType startpunktRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType fagsakYtelseType, KontrollType startpunktRef) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(fagsakYtelseType, FagsakYtelseType.UDEFINERT)) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));
                if (inst.isUnsatisfied()) {
                    continue;
                } else {
                    for (var start : coalesce(startpunktRef, KontrollType.UDEFINERT)) {
                        var sinst = select(cls, inst, new EtterkontrollRefLiteral(start));
                        if (sinst.isResolvable()) {
                            return Optional.of(getInstance(sinst));
                        } else {
                            if (sinst.isAmbiguous()) {
                                throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                                    + fagsakLiteral + ", startpunktRef=" + start);
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

        private static <T> List<T> coalesce(T... vals) {
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        }

    }
}
