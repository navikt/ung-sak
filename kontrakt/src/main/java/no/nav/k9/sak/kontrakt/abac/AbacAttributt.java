package no.nav.k9.sak.kontrakt.abac;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Bruk sammen med @TilpassetAbacAttributt og AbacAttributtSupplier for å definere abac nøkler.
 * Tilgjengelig nøkler er definert i AppAbacAttributtType.
 */
@Target({ METHOD })
@Retention(RUNTIME)
@Documented
public @interface AbacAttributt {

    /** Abac key å knytte resultat av angitt getter til. */
    public String value();

    /** Masker verdier for sporingslogg. */
    public boolean masker() default false;
}
