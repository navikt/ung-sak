package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilter;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class BeregningStegTjeneste {

    public interface FortsettBeregningResultatCallback {
        void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode);
    }

    private final Logger logger = LoggerFactory.getLogger(BeregningStegTjeneste.class);

    private final BeregningTjeneste kalkulusTjeneste;
    private final BeregningsgrunnlagVilkårTjeneste vilkårTjeneste;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;

    @Inject
    public BeregningStegTjeneste(BeregningTjeneste kalkulusTjeneste,
                                 BeregningsgrunnlagVilkårTjeneste vilkårTjeneste,
                                 VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårTjeneste = vilkårTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
    }


    public List<AksjonspunktResultat> fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType) {

        var callback = new SamleAksjonspunktResultater();
        fortsettBeregning(ref, stegType, callback);
        return callback.aksjonspunktResultater;

    }

    public void fortsettBeregningInkludertForlengelser(BehandlingReferanse ref, BehandlingStegType stegType, FortsettBeregningResultatCallback resultatCallback) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        logger.info("Alle perioder til vurdering {}", vilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter));

        fortsettBeregning(ref, stegType, resultatCallback, periodeFilter);
    }

    public void fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType, FortsettBeregningResultatCallback resultatCallback) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        logger.info("Alle perioder til vurdering {}", vilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter));

        periodeFilter.ignorerForlengelseperioder();
        fortsettBeregning(ref, stegType, resultatCallback, periodeFilter);
    }

    private void fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType,
                                   FortsettBeregningResultatCallback resultatCallback,
                                   VilkårPeriodeFilter periodeFilter) {
        periodeFilter.ignorerAvslåttePerioder();
        var perioderTilVurdering = vilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);

        logger.info("Beregner steg {} for perioder {} ", stegType, perioderTilVurdering);

        if (perioderTilVurdering.isEmpty()) {
            return;
        }

        Map<LocalDate, DatoIntervallEntitet> stpTilPeriode = perioderTilVurdering
            .stream()
            .map(PeriodeTilVurdering::getPeriode)
            .map(p -> new AbstractMap.SimpleEntry<>(p.getFomDato(), p))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        var kalkulusResultat = kalkulusTjeneste.beregn(ref, perioderTilVurdering, stegType);

        for (var resultat : kalkulusResultat.getResultater().entrySet()) {
            var eksternReferanse = resultat.getKey();
            var delResultat = resultat.getValue();
            var stp = kalkulusResultat.getStp(eksternReferanse);
            var periode = stpTilPeriode.get(stp);
            resultatCallback.håndter(delResultat, periode);
        }
    }

    static class SamleAksjonspunktResultater implements FortsettBeregningResultatCallback {
        private final List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            aksjonspunktResultater.addAll(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

}
