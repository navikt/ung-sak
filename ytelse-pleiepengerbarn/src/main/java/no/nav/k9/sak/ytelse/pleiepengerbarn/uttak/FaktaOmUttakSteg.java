package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@BehandlingStegRef(kode = "KOFAKUT")
@BehandlingTypeRef
@FagsakYtelseTypeRef("PSB")
public class FaktaOmUttakSteg implements BehandlingSteg {

    protected FaktaOmUttakSteg() {
        // for proxy
    }

    @SuppressWarnings("unused")
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();

        // TODO (FC): åpne aksjonspunkter når følgende
        // 1. sjekk mismatch mellom uttakaktiviteter og godkjente arbeidsforhold fra opptjening/kontroller arbeidsforhold (ta utgangspunkt i
        // godkjent for beregningsgrunnlag?)
        // 2. sjekk om flere arbeisforhold godkjent per arbeidsgiver

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
