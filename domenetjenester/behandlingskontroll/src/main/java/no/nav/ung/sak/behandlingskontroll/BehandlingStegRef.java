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
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef.ContainerOfBehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef.BehandlingTypeRefLiteral;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;

/**
 * Marker type som implementerer interface {@link BehandlingSteg}.<br>
 */
@Repeatable(ContainerOfBehandlingStegRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Documented
public @interface BehandlingStegRef {

    /**
     * Kode-verdi som identifiserer behandlingsteget.
     * <p>
     * Må matche ett innslag i <code>BEHANDLING_STEG_TYPE</code> tabell for å kunne kjøres.
     *
     * @see BehandlingStegType
     */
    BehandlingStegType value();


    /**
     * AnnotationLiteral som kan brukes i CDI søk.
     * <p>
     * Eks. for bruk i:<br>
     * {@link CDI#current#select(jakarta.enterprise.util.TypeLiteral, java.lang.annotation.Annotation...)}.
     */
    public static class BehandlingStegRefLiteral extends AnnotationLiteral<BehandlingStegRef> implements BehandlingStegRef {

        private BehandlingStegType stegtype;

        public BehandlingStegRefLiteral() {
            throw new IllegalArgumentException("Type er obligatorisk");
        }

        public BehandlingStegRefLiteral(BehandlingStegType stegtype) {
            this.stegtype = Objects.requireNonNull(stegtype, "stegtype");
        }

        @Override
        public BehandlingStegType value() {
            return stegtype;
        }

    }

    @SuppressWarnings("unchecked")
    public final static class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType, BehandlingStegType behandlingStegRef) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType, behandlingStegRef);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType, BehandlingStegType behandlingStegRef) { // NOSONAR
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

                        var cinst = select(cls, binst, new BehandlingStegRefLiteral(behandlingStegRef));
                        if (cinst.isResolvable()) {
                            return Optional.of(getInstance(cinst));
                        } else {
                            if (cinst.isAmbiguous()) {
                                throw new IllegalStateException("Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType="
                                    + fagsakLiteral + ", behandlingType=" + behandlingType + ", behandlingStegRef=" + behandlingStegRef);
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
    @Target({ElementType.TYPE})
    @Documented
    public @interface ContainerOfBehandlingStegRef {
        BehandlingStegRef[] value();
    }

}
