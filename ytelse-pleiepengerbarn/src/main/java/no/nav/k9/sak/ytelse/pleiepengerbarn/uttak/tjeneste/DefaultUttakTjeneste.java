package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.pleiepengerbarn.uttak.kontrakter.Simulering;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@Default
@Dependent
public class DefaultUttakTjeneste implements UttakTjeneste {

    private UttakRestKlient restKlient;

    @Inject
    public DefaultUttakTjeneste(UttakRestKlient uttakRestTjeneste) {
        this.restKlient = uttakRestTjeneste;
    }

    @Override
    public Uttaksplan hentUttaksplan(UUID behandlingId, boolean slåSammenLikePerioder) {
        return restKlient.hentUttaksplan(behandlingId, slåSammenLikePerioder);
    }

    @Override
    public Simulering simulerUttaksplan(Uttaksgrunnlag request) {
        return restKlient.simulerUttaksplan(request);
    }

    @Override
    public Uttaksplan opprettUttaksplan(Uttaksgrunnlag request) {
        return restKlient.opprettUttaksplan(request);
    }

    @Override
    public void slettUttaksplan(UUID behandlingId) {
        restKlient.slettUttaksplan(behandlingId);
    }
}
