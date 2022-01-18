package no.nav.k9.sak.mottak.dokumentmottak;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Repeatable(DokumentGruppeRef.ContainerOfDokumentGruppeRef.class)
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface DokumentGruppeRef {

    /**
     * Settes til kode på dokumentgruppe slik det defineres i KODELISTE-tabellen.
     * Av historiske årsaker brukes brevkode kodeverket her ved oppslag
     * 
     * @see @Brevkode
     */
    String value();

    /** AnnotationLiteral som kan brukes ved CDI søk. */
    class DokumentGruppeRefLiteral extends AnnotationLiteral<DokumentGruppeRef> implements DokumentGruppeRef {

        private String kode;

        DokumentGruppeRefLiteral(String kode) {
            this.kode = Objects.requireNonNull(kode, "Brevkode.kode");
        }

        @Override
        public String value() {
            return kode;
        }
    }

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
    @Documented
    public @interface ContainerOfDokumentGruppeRef {
        DokumentGruppeRef[] value();
    }

}
