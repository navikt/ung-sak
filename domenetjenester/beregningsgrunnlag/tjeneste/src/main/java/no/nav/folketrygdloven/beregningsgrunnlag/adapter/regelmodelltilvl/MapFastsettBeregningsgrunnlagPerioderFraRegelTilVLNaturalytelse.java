package no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.kodeverk.MapPeriodeÅrsakFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandlingslager.Kopimaskin;

@ApplicationScoped
public class MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse extends MapFastsettBeregningsgrunnlagPerioderFraRegelTilVL {

    @Inject
    public MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse() {
        // For CDI
    }

    @Override
    protected void mapAndeler(BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, SplittetPeriode splittetPeriode, List<BeregningsgrunnlagPrStatusOgAndel> andelListe, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode) {
        andelListe.forEach(eksisterendeAndel -> mapEksisterendeAndel(splittetPeriode, beregningsgrunnlagPeriode, eksisterendeAndel));
    }

    @Override
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
            .medBeregningsgrunnlagPeriode(splittetPeriode.getPeriode().getFom(), periodeTom);
        splittetPeriode.getPeriodeÅrsaker().stream()
            .map(MapPeriodeÅrsakFraRegelTilVL::map)
            .forEach(bgPeriodeBuilder::leggTilPeriodeÅrsak);
        var beregningsgrunnlagPeriode = bgPeriodeBuilder.build(nyttBeregningsgrunnlag);
        mapAndeler(nyttBeregningsgrunnlag, splittetPeriode, andelListe, beregningsgrunnlagPeriode);
    }

    private void mapEksisterendeAndel(SplittetPeriode splittetPeriode, BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, BeregningsgrunnlagPrStatusOgAndel eksisterendeAndel) {
        BeregningsgrunnlagPrStatusOgAndel nyAndel = Kopimaskin.deepCopy(eksisterendeAndel);
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder(nyAndel);

        Optional<BeregningsgrunnlagPrArbeidsforhold> regelMatchOpt = splittetPeriode.getEksisterendePeriodeAndeler().stream()
            .filter(andel -> andel.getAndelNr().equals(eksisterendeAndel.getAndelsnr()))
            .findFirst();
        regelMatchOpt.ifPresent(regelAndel -> {
            BGAndelArbeidsforhold andelArbeidsforhold = nyAndel.getBgAndelArbeidsforhold().orElseThrow();
            BGAndelArbeidsforhold.Builder andelArbeidsforholdBuilder = BGAndelArbeidsforhold.builder(andelArbeidsforhold)
                .medNaturalytelseBortfaltPrÅr(regelAndel.getNaturalytelseBortfaltPrÅr().orElse(null))
                .medNaturalytelseTilkommetPrÅr(regelAndel.getNaturalytelseTilkommetPrÅr().orElse(null));
            andelBuilder.medBGAndelArbeidsforhold(andelArbeidsforholdBuilder);
        });
        andelBuilder
            .build(beregningsgrunnlagPeriode);
    }

}
