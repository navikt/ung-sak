package no.nav.foreldrepenger.domene.uttak;

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

    // bruker inntil vi får koblet inn uttakRestTjeneste kall
    private UttakInMemoryTjeneste uttakInMemoryTjeneste = new UttakInMemoryTjeneste();

    protected DefaultUttakTjeneste() {
    }

    @Inject
    public DefaultUttakTjeneste(UttakRestTjeneste uttakRestTjeneste) {
        this.uttakRestTjeneste = uttakRestTjeneste;
    }

    @Override
    public boolean harAvslåttUttakPeriode(UUID behandlingUuid) {
        var uttaksplanOpt = hentUttaksplanHvisEksisterer(behandlingUuid);
        return uttaksplanOpt.map(ut -> ut.harAvslåttePerioder()).orElse(false);
    }

    @Override
    public Optional<Uttaksplan> hentUttaksplanHvisEksisterer(UUID behandlingUuid) {
        if (true) {
            return uttakInMemoryTjeneste.hentUttaksplanHvisEksisterer(behandlingUuid); // FIXME K9: Fjern dette
        }
        return uttakRestTjeneste.hentUttaksplan(behandlingUuid);
    }
}
