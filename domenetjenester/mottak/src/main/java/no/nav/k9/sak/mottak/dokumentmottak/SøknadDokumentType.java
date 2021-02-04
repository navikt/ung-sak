package no.nav.k9.sak.mottak.dokumentmottak;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

import no.nav.k9.søknad.ytelse.Ytelse;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface SøknadDokumentType {

    /**
     * Settes til kode på dokumentgruppe slik det defineres i KODELISTE-tabellen.
     * Av historiske årsaker brukes brevkode kodeverket her ved oppslag
     *
     * @see @Brevkode
     */
    Class<? extends Ytelse> value();

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class SøknadDokumentTypeLiteral extends AnnotationLiteral<SøknadDokumentType> implements SøknadDokumentType {

        private Class<? extends Ytelse> kode;

        SøknadDokumentTypeLiteral(Class<? extends Ytelse> kode) {
            this.kode = Objects.requireNonNull(kode, "ytelse");
        }

        @Override
        public Class<? extends Ytelse> value() {
            return kode;
        }
    }
}
