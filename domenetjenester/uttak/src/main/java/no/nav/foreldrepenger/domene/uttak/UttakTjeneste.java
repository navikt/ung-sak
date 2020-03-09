package no.nav.foreldrepenger.domene.uttak;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

public interface UttakTjeneste {

    boolean harAvslåttUttakPeriode(UUID behandlingUuid);

    Optional<Uttaksplan> hentUttaksplanHvisEksisterer(UUID behandlingUuid);

}