package no.nav.k9.sak.domene.uttak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.sak.domene.uttak.input.UttakInput;
import no.nav.k9.sak.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

public interface UttakTjeneste {

    boolean harAvslåttUttakPeriode(UUID behandlingUuid);

    List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid);

    Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid);

    Uttaksplan opprettUttaksplan(UttakInput input);

}