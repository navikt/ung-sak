package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.uttak.repo.UttakRepository;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    private UttakRepository uttakRepository;

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @Inject
    public FaktaOmUttakSteg(UttakRepository uttakRepository) {
        this.uttakRepository = uttakRepository;
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        var oppgittUttak = uttakRepository.hentOppgittUttak(behandlingId);

        // TODO (FC): åpne aksjonspunkter når følgende
        // 1. sjekk mismatch mellom uttakaktiviteter og godkjente arbeidsforhold fra opptjening/kontroller arbeidsforhold (ta utgangspunkt i
        // godkjent for beregningsgrunnlag?)
        // 2. sjekk om flere arbeisforhold godkjent per arbeidsgiver

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
