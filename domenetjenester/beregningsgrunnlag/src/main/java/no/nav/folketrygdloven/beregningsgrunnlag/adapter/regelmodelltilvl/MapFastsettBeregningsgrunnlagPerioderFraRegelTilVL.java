package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapPeriodeÅrsakFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SplittetPeriode;
import no.nav.vedtak.konfig.Tid;
abstract class MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL {

    public BeregningsgrunnlagEntitet mapFraRegel(List<SplittetPeriode> splittedePerioder, String regelinputPeriodisering, BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {

        BeregningsgrunnlagEntitet nyttBeregningsgrunnlag = vlBeregningsgrunnlag.dypKopi();
        BeregningsgrunnlagEntitet.builder(nyttBeregningsgrunnlag)
            .medRegelinputPeriodisering(regelinputPeriodisering)
            .fjernAllePerioder();

        splittedePerioder.forEach(splittetPeriode -> mapSplittetPeriode(nyttBeregningsgrunnlag, splittetPeriode, vlBeregningsgrunnlag));
        return nyttBeregningsgrunnlag;
    }

    protected void mapSplittetPeriode(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                         SplittetPeriode splittetPeriode,
                                                         BeregningsgrunnlagEntitet beregningsgrunnlag) {
        LocalDate periodeTom = utledPeriodeTom(splittetPeriode);

        var originalPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .filter(p -> p.getPeriode().inkluderer(splittetPeriode.getPeriode().getFom()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Ingen matchende perioder"));
        var andelListe = originalPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        var bgPeriodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(splittetPeriode.getPeriode().getFom(), periodeTom)
            .medRegelEvalueringForeslå(originalPeriode.getRegelInput(), originalPeriode.getRegelEvaluering())
            .medRegelEvalueringVilkårsvurdering(originalPeriode.getRegelInputVilkårvurdering(), originalPeriode.getRegelEvalueringVilkårvurdering());
        splittetPeriode.getPeriodeÅrsaker().stream()
            .map(MapPeriodeÅrsakFraRegelTilVL::map)
            .forEach(bgPeriodeBuilder::leggTilPeriodeÅrsak);
        var beregningsgrunnlagPeriode = bgPeriodeBuilder.build(nyttBeregningsgrunnlag);
        mapAndeler(nyttBeregningsgrunnlag, splittetPeriode, andelListe, beregningsgrunnlagPeriode);
    }

    protected abstract void mapAndeler(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, SplittetPeriode splittetPeriode, List<BeregningsgrunnlagPrStatusOgAndel> andelListe, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode);

    LocalDate utledPeriodeTom(SplittetPeriode splittetPeriode) {
        LocalDate tom = splittetPeriode.getPeriode().getTom();
        if (Tid.TIDENES_ENDE.equals(tom)) {
            return null;
        }
        return tom;
    }

    Optional<BeregningsgrunnlagPrArbeidsforhold> finnEksisterendeAndelFraRegel(SplittetPeriode splittetPeriode, BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel) {
        return splittetPeriode.getEksisterendePeriodeAndeler().stream()
            .filter(andel -> andel.getAndelNr().equals(eksisterendeAndel.getAndelsnr()))
            .findFirst();
    }

}
