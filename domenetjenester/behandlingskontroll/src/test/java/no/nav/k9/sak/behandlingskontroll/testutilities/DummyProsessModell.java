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
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

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
        modellBuilder
            .medSteg(BehandlingStegType.KONTROLLER_FAKTA, StartpunktType.KONTROLLER_FAKTA)
            .medSteg(BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT)
            .medSteg(BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR)

            .medSteg(BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, StartpunktType.BEREGNING)
            .medSteg(BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)
            .medSteg(BehandlingStegType.BEREGN_YTELSE)
            .medSteg(BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
