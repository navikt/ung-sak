package no.nav.ung.sak.mottak.dokumentmottak.oppgavebekreftelse;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.k9.oppgave.bekreftelse.Bekreftelse.Type;

/**
 * Marker type som implementerer interface {@link OppgaveTypeRef}.
 */
@Repeatable(OppgaveTypeRef.ContainerOfOppgaveTypeRef.class)
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface OppgaveTypeRef {

    /**
     * {@link Type}
     */
    Type value();


    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
    @Documented
    public @interface ContainerOfOppgaveTypeRef {
        OppgaveTypeRef[] value();
    }

    class OppgaveTypeRefLiteral extends AnnotationLiteral<OppgaveTypeRef> implements OppgaveTypeRef {

        private final Type type;

        OppgaveTypeRefLiteral(Type type) {
            this.type = Objects.requireNonNull(type, "Type");
        }

        @Override
        public Type value() {
            return type;
        }
    }

}
