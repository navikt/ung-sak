package no.nav.ung.abac;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bruk sammen med @TilpassetAbacAttributt og AbacAttributtSupplier for å definere abac nøkler.
 * Tilgjengelig nøkler er definert i AppAbacAttributtType.
 */
@Target({METHOD})
@Retention(RUNTIME)
@Documented
public @interface AppAbacAttributt {

    /**
     * Abac key å knytte resultat av angitt getter til.
     */
    public AppAbacAttributtType value();
}
