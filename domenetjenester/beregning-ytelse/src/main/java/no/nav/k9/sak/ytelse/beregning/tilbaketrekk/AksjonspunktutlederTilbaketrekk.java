package no.nav.k9.sak.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;

@ApplicationScoped
public class AksjonspunktutlederTilbaketrekk implements AksjonspunktUtleder {

    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;

    AksjonspunktutlederTilbaketrekk() {
    }

    @Inject
    public AksjonspunktutlederTilbaketrekk(BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste) {
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
    }

    @Override
    public List<AksjonspunktResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        if (skalVurdereTilbaketrekk(param.getRef())) {
            AksjonspunktResultat aksjonspunktResultat = AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_TILBAKETREKK);
            aksjonspunktResultater.add(aksjonspunktResultat);
        }

        return aksjonspunktResultater;
    }

    private boolean skalVurdereTilbaketrekk(BehandlingReferanse ref) {
        LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje =  beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(ref);
        return VurderBehovForÅHindreTilbaketrekkV2.skalVurdereTilbaketrekk(brAndelTidslinje);
    }
}
