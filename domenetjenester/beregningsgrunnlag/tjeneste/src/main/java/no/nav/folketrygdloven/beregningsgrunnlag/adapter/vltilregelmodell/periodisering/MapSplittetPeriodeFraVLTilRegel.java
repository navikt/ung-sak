package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapArbeidsforholdFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapPeriodeÅrsakFraVlTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SplittetPeriode;

class MapSplittetPeriodeFraVLTilRegel {
    private MapSplittetPeriodeFraVLTilRegel() {
        // skjul public constructor
    }

    static SplittetPeriode map(BeregningsgrunnlagPeriode periode) {
        return SplittetPeriode.builder()
            .medFørstePeriodeAndeler(periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> a.getBgAndelArbeidsforhold().isPresent())
                .map(MapSplittetPeriodeFraVLTilRegel::mapToBeregningsgrunnlagPrArbeidsforhold).collect(Collectors.toList()))
            .medPeriode(Periode.of(periode.getBeregningsgrunnlagPeriodeFom(), periode.getBeregningsgrunnlagPeriodeTom()))
            .medPeriodeÅrsaker(periode.getPeriodeÅrsaker().stream().map(MapPeriodeÅrsakFraVlTilRegel::map).collect(Collectors.toList()))
            .build();
    }

    private static BeregningsgrunnlagPrArbeidsforhold mapToBeregningsgrunnlagPrArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel bgAndel) {
        BGAndelArbeidsforhold bgAndelArbeidsforhold = bgAndel.getBgAndelArbeidsforhold()
            .orElseThrow(() -> new IllegalStateException("Må ha arbeidsforhold"));
        BeregningsgrunnlagPrArbeidsforhold.Builder builder = BeregningsgrunnlagPrArbeidsforhold.builder()
            .medAndelNr(bgAndel.getAndelsnr());
        builder.medArbeidsforhold(MapArbeidsforholdFraVLTilRegel.mapArbeidsforhold(bgAndelArbeidsforhold.getArbeidsgiver(), bgAndelArbeidsforhold.getArbeidsforholdRef()));
        return builder.build();
    }
}
