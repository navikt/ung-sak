package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapArbeidsforholdFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.kodeverk.MapAktivitetStatusV2FraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AndelGraderingImpl;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;

public final class MapAndelGradering {
    private MapAndelGradering() {
        // private constructor
    }

    public static AndelGraderingImpl mapTilRegelAndelGradering(AndelGradering andelGradering) {
        var regelAktivitetStatus = MapAktivitetStatusV2FraVLTilRegel.map(andelGradering.getAktivitetStatus(), null);
        List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Gradering> graderinger = mapGraderingPerioder(andelGradering.getGraderinger());
        AndelGraderingImpl.Builder builder = AndelGraderingImpl.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medGraderinger(graderinger);
        builder.medAndelsnr(andelGradering.getAndelsnr());

        if (andelGradering.getArbeidsgiver() != null) {
            builder.medArbeidsforhold(MapArbeidsforholdFraVLTilRegel.mapArbeidsforhold(andelGradering.getArbeidsgiver(), andelGradering.getArbeidsforholdRef()));
        }
        return builder.build();
    }

    private static List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Gradering> mapGraderingPerioder(List<Gradering> graderingList) {
        return graderingList.stream()
            .map(gradering -> new no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Gradering(
                Periode.of(gradering.getPeriode().getFomDato(), gradering.getPeriode().getTomDato()),
                gradering.getArbeidstidProsent()))
            .collect(Collectors.toList());
    }
}
