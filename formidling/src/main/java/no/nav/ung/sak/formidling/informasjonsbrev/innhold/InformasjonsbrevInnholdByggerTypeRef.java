package no.nav.ung.sak.formidling.informasjonsbrev.innhold;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;

import java.lang.annotation.*;
import java.util.Objects;

/**
 * Marker type som implementerer interface {@link InformasjonsbrevInnholdByggerTypeRef}.
 * Brukes til å velge riktig bygger basert på enum {@link InformasjonsbrevMalType}.
 * For informasjonsbrev så er det 1-1 mellom enum og bygger (for vedtaksbrev så må det utledes fra ett sett med regler)
 */
@Repeatable(InformasjonsbrevInnholdByggerTypeRef.ContainerOfInformasjonsbrevInnholdByggerTypeRef.class)
@Qualifier
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
public @interface InformasjonsbrevInnholdByggerTypeRef {

    /**
     * {@link InformasjonsbrevMalType}
     */
    InformasjonsbrevMalType value();


    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
    @Documented
    public @interface ContainerOfInformasjonsbrevInnholdByggerTypeRef {
        InformasjonsbrevInnholdByggerTypeRef[] value();
    }

    class InformasjonsbrevInnholdByggerTypeRefLiteral extends AnnotationLiteral<InformasjonsbrevInnholdByggerTypeRef> implements InformasjonsbrevInnholdByggerTypeRef {

        private final InformasjonsbrevMalType type;

        public InformasjonsbrevInnholdByggerTypeRefLiteral(InformasjonsbrevMalType type) {
            this.type = Objects.requireNonNull(type, "Type");
        }

        @Override
        public InformasjonsbrevMalType value() {
            return type;
        }
    }

}
