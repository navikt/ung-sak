package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste;

import java.util.UUID;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksgrunnlag;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@Dependent
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {

    private UttakRestKlient restKlient;

    DefaultUttakTjeneste() {
        // CDI
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestKlient uttakRestTjeneste) {
        this.restKlient = uttakRestTjeneste;
    }

    @Override
    public Uttaksplan hentUttaksplan(UUID behandlingId) {
        return restKlient.hentUttaksplan(behandlingId);
    }

    @Override
    public Uttaksplan opprettUttaksplan(Uttaksgrunnlag request) {
        return restKlient.opprettUttaksplan(request);
    }
}
