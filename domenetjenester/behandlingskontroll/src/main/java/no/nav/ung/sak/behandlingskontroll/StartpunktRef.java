package no.nav.ung.sak.behandlingskontroll;

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
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef.BehandlingTypeRefLiteral;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;
import no.nav.ung.sak.behandlingskontroll.StartpunktRef.ContainerOfStartpunktRef;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

/**
 * Marker type som implementerer interface {@link BehandlingSteg} for å skille ulike implementasjoner av samme steg for ulike
 * behandlingtyper.<br>
 *
 * NB: Settes kun dersom det er flere implementasjoner av med samme {@link BehandlingStegRef}.
 */
@Repeatable(ContainerOfStartpunktRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface StartpunktRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandling typer.
     * <p>
     * Må matche ett innslag i <code>BEHANDling_TYPE</code> tabell for å kunne kjøres.
     *
     * @see BehandlingType
     */
    StartpunktType value() default StartpunktType.UDEFINERT;

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    public static class StartpunktRefLiteral extends AnnotationLiteral<StartpunktRef> implements StartpunktRef {

        private StartpunktType navn;

        public StartpunktRefLiteral() {
            this.navn = StartpunktType.UDEFINERT;
        }

        public StartpunktRefLiteral(StartpunktType navn) {
            this.navn = (navn == null ? StartpunktType.UDEFINERT : navn);
        }

        @Override
        public StartpunktType value() {
            return navn;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType, StartpunktType startpunktRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType, startpunktRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, StartpunktType startpunktRef) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(fagsakYtelseType, FagsakYtelseType.UDEFINERT)) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));
                if (inst.isUnsatisfied()) {
                    continue;
                } else {
                    for (var behandlingLiteral : coalesce(behandlingType, BehandlingType.UDEFINERT)) {
                        var binst = select(cls, inst, new BehandlingTypeRefLiteral(behandlingLiteral));
                        if (binst.isUnsatisfied()) {
                            continue;
                        }
                        for (var start : coalesce(startpunktRef, StartpunktType.UDEFINERT)) {
                            var sinst = select(cls, binst, new StartpunktRefLiteral(start));
                            if (sinst.isResolvable()) {
                                return Optional.of(getInstance(sinst));
                            } else {
                                if (sinst.isAmbiguous()) {
                                    throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                                        + fagsakLiteral + ", behandlingType=" + behandlingType + ", startpunktRef=" + start);
                                }
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

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
    @Documented
    public @interface ContainerOfStartpunktRef {
        StartpunktRef[] value();
    }
}
