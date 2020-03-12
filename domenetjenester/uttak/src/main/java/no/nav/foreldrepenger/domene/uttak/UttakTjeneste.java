package no.nav.foreldrepenger.domene.uttak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

public interface UttakTjeneste {

    boolean harAvslåttUttakPeriode(UUID behandlingUuid);

    List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid);

    Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid);

    Uttaksplan opprettUttaksplan(UttakInput input);

}