package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.SammenligningsgrunnlagPrStatusDto;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.PeriodeÅrsak;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;


public class KalkulatorTilBGMapper {
    public static BeregningsgrunnlagAktivitetStatus.Builder mapAktivitetStatus(no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus fraKalkulus) {
        BeregningsgrunnlagAktivitetStatus.Builder builder = new BeregningsgrunnlagAktivitetStatus.Builder();
        builder.medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getKode()));

        return builder;
    }

    public static BeregningsgrunnlagPeriode.Builder mapBeregningsgrunnlagPeriode(BeregningsgrunnlagPeriodeDto fraKalkulus) {
        BeregningsgrunnlagPeriode.Builder builder = new BeregningsgrunnlagPeriode.Builder();

        //med
        builder.medAvkortetPrÅr(fraKalkulus.getAvkortetPrÅr());
        builder.medBeregningsgrunnlagPeriode(fraKalkulus.getBeregningsgrunnlagPeriodeFom(), fraKalkulus.getBeregningsgrunnlagPeriodeTom());
        builder.medBruttoPrÅr(fraKalkulus.getBruttoPrÅr());
        builder.medRedusertPrÅr(fraKalkulus.getRedusertPrÅr());

        //legg til
        fraKalkulus.getPeriodeÅrsaker().forEach(periodeÅrsak -> builder.leggTilPeriodeÅrsak(PeriodeÅrsak.fraKode(periodeÅrsak.getKode())));
        fraKalkulus.getBeregningsgrunnlagPrStatusOgAndelList().forEach(statusOgAndel -> builder.leggTilBeregningsgrunnlagPrStatusOgAndel(mapStatusOgAndel(statusOgAndel)));

        return builder;
    }

    public static SammenligningsgrunnlagPrStatus.Builder mapSammenligningsgrunnlagMedStatus(SammenligningsgrunnlagPrStatusDto fraKalkulus) {
        SammenligningsgrunnlagPrStatus.Builder builder = new SammenligningsgrunnlagPrStatus.Builder();
        builder.medAvvikPromille(fraKalkulus.getAvvikPromilleNy().longValue());
        builder.medRapportertPrÅr(fraKalkulus.getRapportertPrÅr());
        builder.medSammenligningsgrunnlagType(SammenligningsgrunnlagType.fraKode(fraKalkulus.getSammenligningsgrunnlagType().getKode()));
        builder.medSammenligningsperiode(fraKalkulus.getSammenligningsperiodeFom(), fraKalkulus.getSammenligningsperiodeTom());
        return builder;
    }

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapStatusOgAndel(BeregningsgrunnlagPrStatusOgAndelDto fraKalkulus) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.fraKode(fraKalkulus.getAktivitetStatus().getKode()))
            .medArbforholdType(fraKalkulus.getArbeidsforholdType() == null ? null : OpptjeningAktivitetType.fraKode(fraKalkulus.getArbeidsforholdType().getKode()))
            .medBruttoPrÅr(fraKalkulus.getBruttoPrÅr())
            .medRedusertBrukersAndelPrÅr(fraKalkulus.getRedusertBrukersAndelPrÅr())
            .medRedusertRefusjonPrÅr(fraKalkulus.getRedusertRefusjonPrÅr())
            .medInntektskategori(fraKalkulus.getInntektskategori() == null ? null : Inntektskategori.fraKode(fraKalkulus.getInntektskategori().getKode()));

        if (fraKalkulus.getBeregningsperiodeFom() != null) {
            builder.medBeregningsperiode(fraKalkulus.getBeregningsperiodeFom(), fraKalkulus.getBeregningsperiodeTom());
        }

        if (fraKalkulus.getBgAndelArbeidsforhold() != null) {
            builder.medBGAndelArbeidsforhold(KalkulatorTilBGMapper.magBGAndelArbeidsforhold(fraKalkulus.getBgAndelArbeidsforhold()));
        }

        return builder;
    }

    private static BGAndelArbeidsforhold.Builder magBGAndelArbeidsforhold(no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.fastsatt.BGAndelArbeidsforhold fraKalkulus) {
        BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold.builder();
        builder.medArbeidsforholdRef(InternArbeidsforholdRef.ref(fraKalkulus.getArbeidsforholdRef()));
        builder.medArbeidsgiver(MapFraKalkulusTilK9IAY.mapArbeidsgiver(fraKalkulus.getArbeidsgiver()));
        builder.medArbeidsperiodeFom(fraKalkulus.getArbeidsperiodeFom());
        builder.medRefusjonskravPrÅr(fraKalkulus.getRefusjonskravPrÅr());

        builder.medArbeidsperiodeTom(fraKalkulus.getArbeidsperiodeTom());
        builder.medNaturalytelseTilkommetPrÅr(fraKalkulus.getNaturalytelseTilkommetPrÅr());
        builder.medNaturalytelseBortfaltPrÅr(fraKalkulus.getNaturalytelseBortfaltPrÅr());
        return builder;
    }
}
