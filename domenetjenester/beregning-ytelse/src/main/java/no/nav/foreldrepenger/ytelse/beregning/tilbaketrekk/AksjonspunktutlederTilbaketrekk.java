package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

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
