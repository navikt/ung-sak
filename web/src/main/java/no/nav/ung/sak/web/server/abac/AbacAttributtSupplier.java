package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.ung.abac.AppAbacAttributt;
import no.nav.ung.abac.StandardAbacAttributt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

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

    @Override
    public AbacDataAttributter apply(Object obj) {
        var abac = AbacDataAttributter.opprett();
        if (obj == null) {
            return abac;
        }
        if (obj instanceof Collection) {
            for (var part : (Collection<?>) obj) {
                leggTilAbacAttributter(part, abac);
            }
        } else {
            leggTilAbacAttributter(obj, abac);
        }
        return abac;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean leggTilAbacAttributter(Object obj, AbacDataAttributter abac) {
        var cls = obj.getClass();
        boolean erLagtTil = false;
        for (var m : cls.getMethods()) {
            if (m.getParameterCount() > 0) {
                continue;
            }
            var standardAbacAttributtAnnotering = m.getAnnotation(StandardAbacAttributt.class);
            if (standardAbacAttributtAnnotering != null) {
                erLagtTil |= leggTilStandardAbacAttributtVerdier(obj, abac, m, standardAbacAttributtAnnotering);
            }
            var appAbacAttributtAnnotering = m.getAnnotation(AppAbacAttributt.class);
            if (appAbacAttributtAnnotering != null) {
                erLagtTil |= leggTilAppAbacAttributtVerdier(obj, abac, m, appAbacAttributtAnnotering);
            }
        }
        if (!erLagtTil) {
            throw new IllegalStateException("Ingen abac attributter lagt til fra " + cls + ", mangler annotasjoner? " + StandardAbacAttributt.class + " eller " + AppAbacAttributt.class);
        }
        return erLagtTil;
    }

    private static boolean leggTilStandardAbacAttributtVerdier(Object obj, AbacDataAttributter abac, Method m, StandardAbacAttributt standardAbacAttributtAnnotering) {
        var rt = m.getReturnType();
        try {
            var abacAttributtType = Objects.requireNonNull(standardAbacAttributtAnnotering.value(), "attibuttype var null");
            var resultat = m.invoke(obj);
            if (resultat != null) {
                if (Collection.class.isAssignableFrom(rt)) {
                    abac.leggTil(abacAttributtType, (Collection) resultat);
                } else {
                    abac.leggTil(abacAttributtType, resultat);
                }
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Kunne ikke hente ut abac attributter fra " + obj, e);
        }
        return false; //la til ingen verdier
    }

    private static boolean leggTilAppAbacAttributtVerdier(Object obj, AbacDataAttributter abac, Method m, AppAbacAttributt appAbacAttributtAnnotering) {
        var rt = m.getReturnType();
        try {
            var abacAttributtType = Objects.requireNonNull(appAbacAttributtAnnotering.value(), "attibuttype var null");
            var resultat = m.invoke(obj);
            if (resultat != null) {
                if (Collection.class.isAssignableFrom(rt)) {
                    abac.leggTil(abacAttributtType, (Collection) resultat);
                } else {
                    abac.leggTil(abacAttributtType, resultat);
                }
                return true;
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Kunne ikke hente ut abac attributter fra " + obj, e);
        }
        return false; //la til ingen verdier
    }


}
