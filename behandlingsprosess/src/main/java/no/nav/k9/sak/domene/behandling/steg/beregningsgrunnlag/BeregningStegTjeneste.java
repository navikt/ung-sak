package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.output.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@Dependent
public class BeregningStegTjeneste {

    public interface FortsettBeregningResultatCallback {
        void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode);
    }

    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;

    @Inject
    public BeregningStegTjeneste(BeregningTjeneste kalkulusTjeneste, BeregningsgrunnlagVilkårTjeneste vilkårTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public void fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType, FortsettBeregningResultatCallback resultatCallback) {

        var perioderTilVurdering = vilkårTjeneste.utledPerioderTilVurdering(ref, true);

        if (perioderTilVurdering.isEmpty()) {
            return;
        }

        Map<LocalDate, DatoIntervallEntitet> stpTilPeriode = perioderTilVurdering
            .stream()
            .map(p -> new AbstractMap.SimpleEntry<>(p.getFomDato(), p))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        List<LocalDate> skjæringstidspunkter = List.copyOf(stpTilPeriode.keySet());

        var kalkulusResultat = kalkulusTjeneste.fortsettBeregning(ref, skjæringstidspunkter, stegType);

        for (var resultat : kalkulusResultat.getResultater().entrySet()) {
            var eksternReferanse = resultat.getKey();
            var delResultat = resultat.getValue();
            var stp = kalkulusResultat.getSkjæringstidspunkter().get(eksternReferanse);
            var periode = stpTilPeriode.get(stp);
            resultatCallback.håndter(delResultat, periode);
        }
    }

    public List<AksjonspunktResultat> fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType) {

        var callback = new SamleAksjonspunktResultater();
        fortsettBeregning(ref, stegType, callback);
        return callback.aksjonspunktResultater;

    }

    static class SamleAksjonspunktResultater implements FortsettBeregningResultatCallback {
        private final List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
