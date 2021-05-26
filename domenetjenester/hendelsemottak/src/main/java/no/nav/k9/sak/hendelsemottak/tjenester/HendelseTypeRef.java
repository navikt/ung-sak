package no.nav.k9.sak.hendelsemottak.tjenester;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.k9.sak.domene.registerinnhenting.EndringStartpunktUtleder;

/**
 * Marker type som implementerer interface {@link EndringStartpunktUtleder}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface HendelseTypeRef {

    /**
     * Settes til navn på forretningshendelse slik det defineres i KODELISTE-tabellen.
     */
    String value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class HendelseTypeRefLiteral extends AnnotationLiteral<HendelseTypeRef> implements HendelseTypeRef {

        private String navn;

        public HendelseTypeRefLiteral(String navn) {
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

        /**
         * Kan brukes til å finne instanser blant angitte som matcher følgende kode
         */
        public static <I> List<I> list(Instance<I> instances, String hendelseKode) {
            Objects.requireNonNull(instances);
            Objects.requireNonNull(hendelseKode);

            Instance<I> selected = instances.select(new HendelseTypeRefLiteral(hendelseKode));
            if (selected.isUnsatisfied()) {
                return List.of();
            } else if (selected.stream().anyMatch(it -> it.getClass().isAnnotationPresent(Dependent.class))) {
                throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv");
            } else {
                return selected.stream().collect(Collectors.toList());
            }
        }
    }
}
