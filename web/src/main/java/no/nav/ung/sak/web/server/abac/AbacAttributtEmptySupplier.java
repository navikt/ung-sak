package no.nav.ung.sak.web.server.abac;

import java.util.function.Function;

import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;

/**
 * Erklærer at dto ikke har noen egne custom abac attributter.
 *
 * <p>
 *
 * <pre>
 * public void myRestMethod(@NotNull @TilpassetAbacAttributt(supplierClass=AbacAttributtEmptySupplier.class) MyDto dtoWithAbacAttributtes) {
 *       ...
 * }
 *
 * </pre>
 */
public class AbacAttributtEmptySupplier implements Function<Object, AbacDataAttributter> {

    @Override
    public AbacDataAttributter apply(Object obj) {
        return AbacDataAttributter.opprett();
    }
}
