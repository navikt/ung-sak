package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.UUID;

import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

public interface UttakTjeneste {

    Uttaksplan hentUttaksplan(UUID behandlingId);

    Uttaksplan opprettUttaksplan(Uttaksgrunnlag request);

}
