package no.nav.k9.sak.mottak.dokumentmottak;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.k9.kodeverk.dokument.Brevkode;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface DokumentGruppeRef {

    /**
     * Settes til navn på dokumentgruppe slik det defineres i KODELISTE-tabellen.
     * Av historiske årsaker brukes brevkode kodeverket her.
     */
    Brevkode value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class DokumentGruppeRefLiteral extends AnnotationLiteral<DokumentGruppeRef> implements DokumentGruppeRef {

        private Brevkode navn;

        DokumentGruppeRefLiteral(Brevkode navn) {
            this.navn = navn;
        }

        @Override
        public Brevkode value() {
            return navn;
        }
    }
}
