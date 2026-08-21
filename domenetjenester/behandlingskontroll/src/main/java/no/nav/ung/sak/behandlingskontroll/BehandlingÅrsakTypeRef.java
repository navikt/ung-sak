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

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef.ContainerOfBehandlingÅrsakTypeRef;

/**
 * Marker type som skiller ulike implementasjoner av samme funksjonalitet for ulike {@link BehandlingÅrsakType}.<br>
 *
 * NB: Settes kun dersom det er flere implementasjoner med samme funksjonalitet.
 */
@Repeatable(ContainerOfBehandlingÅrsakTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@Documented
public @interface BehandlingÅrsakTypeRef {

    /**
     * Kode-verdi som skiller ulike implementasjoner for ulike behandlingårsak typer.
     * <p>
     * Må matche ett innslag i <code>BEHANDLING_ÅRSAK_TYPE</code> tabell for å kunne kjøres.
     *
     * @see BehandlingÅrsakType
     */
    BehandlingÅrsakType value() default BehandlingÅrsakType.UDEFINERT;

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    public static class BehandlingÅrsakTypeRefLiteral extends AnnotationLiteral<BehandlingÅrsakTypeRef> implements BehandlingÅrsakTypeRef {

        private BehandlingÅrsakType behandlingÅrsakType;

        public BehandlingÅrsakTypeRefLiteral() {
            this.behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;
        }

        public BehandlingÅrsakTypeRefLiteral(BehandlingÅrsakType behandlingÅrsakType) {
            if (behandlingÅrsakType == null) {
                this.behandlingÅrsakType = BehandlingÅrsakType.UDEFINERT;
            } else {
                this.behandlingÅrsakType = behandlingÅrsakType;
            }
        }

        @Override
        public BehandlingÅrsakType value() {
            return behandlingÅrsakType;
        }
    }

    @SuppressWarnings("unchecked")
    public static final class Lookup {

        private Lookup() {
        }

        public static <I> Optional<I> find(Class<I> cls, BehandlingÅrsakType behandlingÅrsakType) {
            return find(cls, (CDI<I>) CDI.current(), behandlingÅrsakType);
        }

        public static <I> I get(Class<I> cls, Instance<I> instances, BehandlingÅrsakType behandlingÅrsakType) {
            var result = find(cls, instances, behandlingÅrsakType);

            return result.orElseThrow(
                () -> new UnsupportedOperationException("Har ikke " + cls.getSimpleName() + " for behandlingÅrsakType=" + behandlingÅrsakType + ", blant:" + instances));
        }

        public static <I> Optional<I> find(Class<I> cls, Instance<I> instances, BehandlingÅrsakType behandlingÅrsakType) { // NOSONAR
            Objects.requireNonNull(instances, "instances");

            for (var behandlingÅrsakLiteral : coalesce(behandlingÅrsakType, BehandlingÅrsakType.UDEFINERT)) {
                var inst = select(cls, instances, new BehandlingÅrsakTypeRefLiteral(behandlingÅrsakLiteral));
                if (inst.isResolvable()) {
                    return Optional.of(getInstance(inst));
                } else {
                    if (inst.isAmbiguous()) {
                        throw new IllegalStateException(
                            "Har flere matchende instanser for klasse : " + cls.getName() + ", behandlingÅrsakType=" + behandlingÅrsakType + ", instanser=" + inst);
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

        private static <T> List<T> coalesce(T... vals) {
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
    public @interface ContainerOfBehandlingÅrsakTypeRef {
        BehandlingÅrsakTypeRef[] value();
    }
}
