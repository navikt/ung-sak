package no.nav.k9.sak.ytelse.pleiepengerbarn.henleggelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggelsePostopsTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.UttakRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
class PSBHenleggelsePostopsTjeneste implements HenleggelsePostopsTjeneste {

    private UttakRestKlient uttakRestKlient;

    public PSBHenleggelsePostopsTjeneste() {
        // For CDI
    }

    @Inject
    PSBHenleggelsePostopsTjeneste(UttakRestKlient uttakRestKlient) {
        this.uttakRestKlient = uttakRestKlient;
    }

    @Override
    public void utfør(Behandling behandling) {
        uttakRestKlient.slettUttaksplan(behandling.getUuid());
    }
}
