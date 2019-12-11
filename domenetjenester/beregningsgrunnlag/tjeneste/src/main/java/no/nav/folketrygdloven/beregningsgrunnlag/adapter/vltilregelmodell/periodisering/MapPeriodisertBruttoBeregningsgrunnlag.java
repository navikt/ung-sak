package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapArbeidsforholdFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapAktivitetStatusV2FraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AktivitetStatusV2;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodisertBruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.grunnlag.inntekt.Arbeidsforhold;

public final class MapPeriodisertBruttoBeregningsgrunnlag {

    private MapPeriodisertBruttoBeregningsgrunnlag() {
        // skjul default
    }

    public static List<PeriodisertBruttoBeregningsgrunnlag> map(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapPeriodisertBruttoBeregningsgrunnlag::mapPeriode)
            .collect(Collectors.toList());
    }

    private static PeriodisertBruttoBeregningsgrunnlag mapPeriode(BeregningsgrunnlagPeriode bgp) {
        Periode regelPeriode = Periode.of(bgp.getBeregningsgrunnlagPeriodeFom(), bgp.getBeregningsgrunnlagPeriodeTom());
        PeriodisertBruttoBeregningsgrunnlag.Builder periodeBuilder = PeriodisertBruttoBeregningsgrunnlag.builder()
            .medPeriode(regelPeriode);
        bgp.getBeregningsgrunnlagPrStatusOgAndelList().forEach(a ->
            periodeBuilder.leggTilBruttoBeregningsgrunnlag(mapBruttoBG(a))
        );
        return periodeBuilder.build();
    }

    private static BruttoBeregningsgrunnlag mapBruttoBG(BeregningsgrunnlagPrStatusOgAndel a) {
        AktivitetStatusV2 regelAktivitetStatus = MapAktivitetStatusV2FraVLTilRegel.map(
            a.getAktivitetStatus(),
            a.getInntektskategori());

        Optional<Arbeidsforhold> arbeidsforhold = a.getBgAndelArbeidsforhold()
            .map(bga ->
                MapArbeidsforholdFraVLTilRegel.mapArbeidsforhold(
                    bga.getArbeidsgiver(),
                    bga.getArbeidsforholdRef())
            );
        return BruttoBeregningsgrunnlag.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medArbeidsforhold(arbeidsforhold.orElse(null))
            .medBruttoBeregningsgrunnlag(a.getBruttoInkludertNaturalYtelser())
            .build();
    }
}
