package no.nav.k9.sak.mottak.dokumentmottak;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Marker type som implementerer interface {@link Dokumentmottaker}.
 */
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface InntektsmeldingDokumentType {

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    class InntektsmeldingDokumentTypeLiteral extends AnnotationLiteral<InntektsmeldingDokumentType> implements InntektsmeldingDokumentType {

    }
}
