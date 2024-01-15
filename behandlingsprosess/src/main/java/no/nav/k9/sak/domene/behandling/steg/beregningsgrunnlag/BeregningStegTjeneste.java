package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.EndretUtbetalingPeriodeutleder;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class BeregningStegTjeneste {

    public interface FortsettBeregningResultatCallback {
        void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode);
    }

    private final Logger logger = LoggerFactory.getLogger(BeregningStegTjeneste.class);

    private final BeregningTjeneste kalkulusTjeneste;
    private final Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutleder;
    private final BeregningStegPeriodeFilter beregningStegPeriodeFilter;

    @Inject
    public BeregningStegTjeneste(BeregningTjeneste kalkulusTjeneste,
                                 @Any Instance<EndretUtbetalingPeriodeutleder> endretUtbetalingPeriodeutleder,
                                 BeregningStegPeriodeFilter beregningStegPeriodeFilter) {
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.endretUtbetalingPeriodeutleder = endretUtbetalingPeriodeutleder;
        this.beregningStegPeriodeFilter = beregningStegPeriodeFilter;
    }


    public List<AksjonspunktResultat> fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType) {

        var callback = new SamleAksjonspunktResultater();
        fortsettBeregning(ref, stegType, callback);
        return callback.aksjonspunktResultater;

    }

    public void fortsettBeregningInkludertForlengelser(BehandlingReferanse ref, BehandlingStegType stegType, FortsettBeregningResultatCallback resultatCallback) {
        var perioder = beregningStegPeriodeFilter.filtrerPerioder(ref, stegType);

        // Beregner dersom endring i uttak
        var forlengelserMedEndring = perioder.stream()
            .filter(p -> !ingenRelevantEndring(ref, p))
            .collect(Collectors.toCollection(TreeSet::new));

        if (!forlengelserMedEndring.isEmpty()) {
            fortsettBeregning(ref, stegType, resultatCallback, forlengelserMedEndring);
        }

        // Kopierer dersom ingen endring
        var forlengelserUtenEndring = perioder.stream()
            .filter(p -> ingenRelevantEndring(ref, p))
            .collect(Collectors.toCollection(TreeSet::new));

        if (!forlengelserUtenEndring.isEmpty()) {
            kalkulusTjeneste.kopier(ref, forlengelserUtenEndring, stegType);
        }
    }

    public void fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType, FortsettBeregningResultatCallback resultatCallback) {
        var perioder = beregningStegPeriodeFilter.filtrerPerioder(ref, stegType);
        fortsettBeregning(ref, stegType, resultatCallback, perioder);
    }

    private void fortsettBeregning(BehandlingReferanse ref, BehandlingStegType stegType,
                                   FortsettBeregningResultatCallback resultatCallback,
                                   NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {

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

    private boolean ingenRelevantEndring(BehandlingReferanse ref, PeriodeTilVurdering p) {
        return p.erForlengelse() && EndretUtbetalingPeriodeutleder.finnUtleder(endretUtbetalingPeriodeutleder, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .utledPerioder(ref, p.getPeriode()).isEmpty();
    }

    static class SamleAksjonspunktResultater implements FortsettBeregningResultatCallback {
        private final List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();

        @Override
        public void håndter(KalkulusResultat kalkulusResultat, DatoIntervallEntitet periode) {
            var apPrKode = kalkulusResultat.getBeregningAksjonspunktResultat().stream()
                .map(BeregningResultatMapper::map)
                .collect(Collectors.toMap(AksjonspunktResultat::getAksjonspunktDefinisjon, Function.identity(), this::finnSenesteFrist));
            aksjonspunktResultater.addAll(apPrKode.values());
        }

        private AksjonspunktResultat finnSenesteFrist(AksjonspunktResultat ap1, AksjonspunktResultat ap2) {
            if (ap1.getFrist() == null) {
                return ap1;
            }
            if (ap2.getFrist() == null) {
                return ap2;
            }
            return ap1.getFrist().isBefore(ap2.getFrist()) ? ap2 : ap1;
        }
    }

}
