package no.nav.k9.sak.ytelse.omsorgspenger.henleggelse;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggelsePostopsTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
class OMPHenleggelsePostopsTjeneste implements HenleggelsePostopsTjeneste {

    private ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;

    public OMPHenleggelsePostopsTjeneste() {
        // For CDI
    }

    @Inject
    OMPHenleggelsePostopsTjeneste(ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste) {
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;
    }

    @Override
    public void utfør(Behandling behandling) {
        årskvantumDeaktiveringTjeneste.meldFraDersomDeaktivering(behandling);
    }
}
