package no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AksjonspunktutlederTilbaketrekk implements AksjonspunktUtleder {
    private static final String TOGGLE = "fpsak.match.beregningsresultat";

    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private Unleash unleash;

    AksjonspunktutlederTilbaketrekk() {
    }

    @Inject
    public AksjonspunktutlederTilbaketrekk(BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
                                           Unleash unleash) {
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.unleash = unleash;
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
        if (unleash.isEnabled(TOGGLE)) {
            return VurderBehovForÅHindreTilbaketrekkV2.skalVurdereTilbaketrekk(brAndelTidslinje);
        }
        return VurderBehovForÅHindreTilbaketrekk.skalVurdereTilbaketrekk(brAndelTidslinje);
    }
}
