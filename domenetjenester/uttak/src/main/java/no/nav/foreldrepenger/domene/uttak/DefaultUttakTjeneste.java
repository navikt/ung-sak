package no.nav.foreldrepenger.domene.uttak;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.rest.UttakRestTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;

@ApplicationScoped
@Default
public class DefaultUttakTjeneste implements UttakTjeneste {

    private UttakRestTjeneste uttakRestTjeneste;

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestTjeneste uttakRestTjeneste) {
        this.uttakRestTjeneste = uttakRestTjeneste;
    }

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttaksplan(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplan(UUID behandlingUuid) {
        return hentUttaksplaner(behandlingUuid).stream().findFirst();
    }

    @Override
    public List<Uttaksplan> hentUttaksplaner(UUID... behandlingUuid) {
        return uttakRestTjeneste.hentUttaksplaner(behandlingUuid);
    }
}
