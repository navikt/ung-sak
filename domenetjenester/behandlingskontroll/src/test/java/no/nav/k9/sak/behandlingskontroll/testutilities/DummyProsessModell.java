package no.nav.k9.sak.behandlingskontroll.testutilities;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellImpl;

@ApplicationScoped
public class DummyProsessModell {

    private static final String YTELSE = "SVP";
    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.SVANGERSKAPSPENGER;

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-002")
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        modellBuilder.medSteg(
            BehandlingStegType.KONTROLLER_FAKTA,
            BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
           
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG,
            BehandlingStegType.BEREGN_YTELSE,
            BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
