package no.nav.ung.abac;

import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bruk sammen med @TilpassetAbacAttributt og AbacAttributtSupplier for å definere abac nøkler.
 * Tilgjengelig nøkler er definert i AppAbacAttributtType.
 */
@Target({ METHOD })
@Retention(RUNTIME)
@Documented
public @interface StandardAbacAttributt {

    /**
     * Abac key å knytte resultat av angitt getter til.
     */
    public StandardAbacAttributtType value();
}
