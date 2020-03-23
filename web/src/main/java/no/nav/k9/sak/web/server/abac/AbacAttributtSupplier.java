package no.nav.k9.sak.web.server.abac;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import no.nav.k9.abac.AbacAttributt;
import no.nav.k9.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;

/**
 * Mapper om dtoer som har @AbacAttributt for å angi Abac nøkler for sporing, etc.
 *
 * <p>
 *
 * <pre>
 * public void myRestMethod(@NotNull @TilpassetAbacAttributt(supplierClass=AbacAttributtSupplier.class) MyDto dtoWithAbacAttributtes) {
 *       ...
 * }
 *
 *
 *
 * ... somwhere else ...
 * class MyDto {
 *
 *   &#64;AbacAttributt("behandlingId")
 *   public String getBehandlingId() { ... }
 * }
 * </pre>
 */
public class AbacAttributtSupplier implements Function<Object, AbacDataAttributter> {

    private static final Class<AbacAttributt> ANNOTATION_CLASS = AbacAttributt.class;

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public AbacDataAttributter apply(Object obj) {
        var abac = AbacDataAttributter.opprett();
        if (obj == null) {
            return abac;
        }

        boolean erLagtTil = false;
        var cls = obj.getClass();
        for (var m : cls.getMethods()) {
            if (m.getParameterCount() > 0) {
                continue;
            }
            var ann = m.getAnnotation(ANNOTATION_CLASS);
            if (ann == null) {
                continue;
            }

            var rt = m.getReturnType();
            try {
                var key = Objects.requireNonNull(ann.value(), "abac key");

                var resultat = m.invoke(obj);
                if (resultat != null) {
                    var abacAttributtType = toAbacAttributtType(key);
                    if (Collection.class.isAssignableFrom(rt)) {
                        abac.leggTil(abacAttributtType, (Collection) resultat);
                    } else {
                        abac.leggTil(abacAttributtType, resultat);
                    }
                    erLagtTil = true;
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Kunne ikke hente ut abac attributter fra " + obj, e);
            }
        }
        if (!erLagtTil) {
            throw new IllegalStateException("Ingen abac attributter lagt til fra " + cls + ", mangler annotasjoner? " + ANNOTATION_CLASS);
        }
        return abac;
    }
    
    private static final AbacAttributtType toAbacAttributtType(String key) {
        /*
         * XXX: Implementasjoner av AbacAttributtType mangler equals og hashcode. Flere steder, slik
         *      som "AbacDataAttributter.getVerdier", bruker equals+hashCode for sjekk av om et gitt
         *      attributt er satt. Inntil dette er ryddet opp i må kun én instans per key benyttes.
         */
        
        for (AbacAttributtType type : StandardAbacAttributtType.values()) {
            if (type.getSporingsloggKode().equals(key)) {
                return type;
            }
        }
        for (AbacAttributtType type : AppAbacAttributtType.values()) {
            if (type.getSporingsloggKode().equals(key)) {
                return type;
            }
        }
        
        throw new IllegalStateException("Ukjent ABAC-attributt.");
    }

}
