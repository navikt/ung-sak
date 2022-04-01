package no.nav.k9.sak.behandlingskontroll;

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
import jakarta.enterprise.util.Nonbinding;
import jakarta.inject.Qualifier;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef.ContainerOfBehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral;

/**
 * Marker type som implementerer interface {@link BehandlingSteg} for å skille ulike implementasjoner av samme steg for ulike
 * behandlingtyper.<br>
 *
 * NB: Settes kun dersom det er flere implementasjoner av med samme {@link BehandlingStegRef}.
 */
@Repeatable(ContainerOfBehandlingTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface BehandlingTypeRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandling typer.
     * <p>
     * Må matche ett innslag i <code>BEHANDling_TYPE</code> tabell for å kunne kjøres.
     *
     * @see no.nav.k9.kodeverk.behandling.BehandlingType
     */
    @Nonbinding
    BehandlingType[] value() default {};

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    public static class BehandlingTypeRefLiteral extends AnnotationLiteral<BehandlingTypeRef> implements BehandlingTypeRef {

        private BehandlingType[] behandlingType;

        public BehandlingTypeRefLiteral() {
            this.behandlingType = new BehandlingType[0];
        }

        public BehandlingTypeRefLiteral(BehandlingType behandlingType) {
            if (behandlingType == null ) {
                this.behandlingType = new BehandlingType[0];
                return;
            }
            this.behandlingType = new BehandlingType[1];
            this.behandlingType[0] = behandlingType;
        }

        @Override
        public BehandlingType[] value() {
            return behandlingType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            BehandlingTypeRefLiteral that = (BehandlingTypeRefLiteral) o;
            return Arrays.equals(behandlingType, that.behandlingType);
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + Arrays.hashCode(behandlingType);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, String ytelseTypeKode, BehandlingType behandlingType) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType);
        }

        public static <I> Optional<I> find(Class<I> cls, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType) {
            return find(cls, (CDI<I>) CDI.current(), ytelseTypeKode, behandlingType);
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseTypeKode, BehandlingType behandlingType) {
            return find(cls, instances,
                ytelseTypeKode == null ? null : ytelseTypeKode.getKode(),
                behandlingType);
        }

        public static <I> I get(Class<I> cls, Instance<I> instances, FagsakYtelseType ytelseType, BehandlingType behandlingType) {
            var result = find(cls, instances,
                ytelseType == null ? null : ytelseType.getKode(),
                behandlingType);

            return result.orElseThrow(
                () -> new UnsupportedOperationException("Har ikke " + cls.getSimpleName() + " for ytelseType=" + ytelseType + ", behandlingType=" + behandlingType + ", blant:" + instances));
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, String fagsakYtelseType, BehandlingType behandlingType) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var fagsakLiteral : coalesce(fagsakYtelseType, "*")) {
                var inst = select(cls, instances, new FagsakYtelseTypeRefLiteral(fagsakLiteral));

                if (inst.isUnsatisfied()) {
                    continue;
                } else {
                    var binst = select(cls, inst, new BehandlingTypeRefLiteral(behandlingType));
                    if (binst.isResolvable()) {
                        return Optional.of(getInstance(binst));
                    } else {
                        if (binst.isAmbiguous()) {
                            throw new IllegalStateException(
                                "Har flere matchende instanser for klasse : " + cls.getName() + ", fagsakType=" + fagsakLiteral + ", behandlingType=" + behandlingType + ", instanser=" + binst);
                        }
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
            return Arrays.stream(vals).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        }

        private static <I> Instance<I> select(Class<I> cls, Instance<I> instances, Annotation anno) {
            return cls != null
                ? instances.select(cls, anno)
                : instances.select(anno);
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
    public @interface ContainerOfBehandlingTypeRef {
        BehandlingTypeRef[] value();
    }
}
