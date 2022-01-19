package no.nav.k9.sak.behandlingskontroll;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.inject.Stereotype;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import no.nav.k9.sak.behandlingskontroll.VilkårTypeRef.ContainerOfVilkårTypeRef;

@Repeatable(value = ContainerOfVilkårTypeRef.class)
@Qualifier
@Stereotype
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Documented
public @interface VilkårTypeRef {

    /**
     * Settes til navn på vilkår slik det defineres i VILKÅR_TYPE tabellen.
     */
    String value()

        default "*";

    /**
     * container for repeatable annotations.
     *
     * @see https://docs.oracle.com/javase/tutorial/java/annotations/repeating.html
     */
    @Inherited
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
    @Documented
    public @interface ContainerOfVilkårTypeRef {
        VilkårTypeRef[] value();
    }

    /**
     * AnnotationLiteral som kan brukes ved CDI søk.
     */
    public static class VilkårTypeRefLiteral extends AnnotationLiteral<VilkårTypeRef> implements VilkårTypeRef {

        private String navn;

        public VilkårTypeRefLiteral() {
            this("*");
        }

        public VilkårTypeRefLiteral(String navn) {
            this.navn = navn;
        }

        @Override
        public String value() {
            return navn;
        }

    }
}



