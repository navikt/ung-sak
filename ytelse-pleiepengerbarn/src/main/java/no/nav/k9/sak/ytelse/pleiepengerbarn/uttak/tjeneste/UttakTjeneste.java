package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.UUID;

import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

public interface UttakTjeneste {

    Uttaksplan hentUttaksplan(UUID behandlingId, boolean slåSammenLikePerioder);

    Uttaksplan opprettUttaksplan(Uttaksgrunnlag request);

    Simulering simulerUttaksplan(Uttaksgrunnlag request);

    void slettUttaksplan(UUID behandlingId);

}
