package no.nav.foreldrepenger.web.server.abac;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import no.nav.k9.sak.kontrakt.abac.AbacAttributt;
import no.nav.vedtak.sikkerhet.abac.AbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;

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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public AbacDataAttributter apply(Object obj) {
        var abac = AbacDataAttributter.opprett();
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
                    if (Collection.class.isAssignableFrom(rt)) {
                        abac.leggTil(new MyAbac(key), (Collection) resultat);
                    } else {
                        abac.leggTil(new MyAbac(key), resultat);
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

    private static class MyAbac implements AbacAttributtType {

        private String kode;

        public MyAbac(String kode) {
            this.kode = kode;
        }

        @Override
        public String getSporingsloggKode() {
            return kode;
        }

        @Override
        public boolean getMaskerOutput() {
            return false;
        }

    }
}