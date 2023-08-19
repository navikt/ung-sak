package no.nav.k9.sak.ytelse.beregning.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrArbeidsforhold;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.BeregningsgrunnlagPrStatus;
import no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Periode;

@ApplicationScoped
public class MapBeregningsgrunnlagFraVLTilRegel {

    private MapBeregningsgrunnlagFraVLTilRegel() {
    }

    public static no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag map(Beregningsgrunnlag vlBeregningsgrunnlag) {
        List<AktivitetStatus> aktivitetStatuser = vlBeregningsgrunnlag.getAktivitetStatuser().stream()
            .map(vlBGAktivitetStatus -> AktivitetStatusMapper.fraVLTilRegel(vlBGAktivitetStatus.getAktivitetStatus()))
            .collect(Collectors.toList());

        List<BeregningsgrunnlagPeriode> perioder = mapBeregningsgrunnlagPerioder(vlBeregningsgrunnlag);

        return no.nav.k9.sak.ytelse.beregning.regelmodell.beregningsgrunnlag.Beregningsgrunnlag.builder()
            .medSkjæringstidspunkt(vlBeregningsgrunnlag.getSkjæringstidspunkt())
            .medAktivitetStatuser(aktivitetStatuser)
            .medBeregningsgrunnlagPerioder(perioder)
            .build();
    }

    private static List<BeregningsgrunnlagPeriode> mapBeregningsgrunnlagPerioder(Beregningsgrunnlag vlBeregningsgrunnlag) {
        return vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapBeregningsgrunnlagFraVLTilRegel::mapBeregningsgrunnlagPeriode)
            .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriode mapBeregningsgrunnlagPeriode(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        final BeregningsgrunnlagPeriode.Builder regelBGPeriode = BeregningsgrunnlagPeriode.builder()
            .medPeriode(Periode.of(vlBGPeriode.getBeregningsgrunnlagPeriodeFom(), vlBGPeriode.getBeregningsgrunnlagPeriodeTom()))
            .medInntektGraderingsprosent(vlBGPeriode.getInntektGraderingsprosent())
            .medGraderingsfaktorTid(vlBGPeriode.getGraderingsfaktorTid())
            .medGraderingsfaktorInntekt(vlBGPeriode.getGraderingsfaktorInntekt());
        List<BeregningsgrunnlagPrStatus> beregningsgrunnlagPrStatus = mapVLBGPrStatus(vlBGPeriode);
        beregningsgrunnlagPrStatus.forEach(regelBGPeriode::medBeregningsgrunnlagPrStatus);
        regelBGPeriode.medBruttoBeregningsgrunnlag(vlBGPeriode.getBruttoPrÅr());

        return regelBGPeriode.build();
    }

    private static List<BeregningsgrunnlagPrStatus> mapVLBGPrStatus(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode) {
        List<BeregningsgrunnlagPrStatus> liste = new ArrayList<>();
        BeregningsgrunnlagPrStatus bgpsATFL = null;

        for (BeregningsgrunnlagPrStatusOgAndel vlBGPStatus : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            final AktivitetStatus regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(vlBGPStatus.getAktivitetStatus());
            if (AktivitetStatus.ATFL.equals(regelAktivitetStatus)) {
                if (bgpsATFL == null) {  // Alle ATFL håndteres samtidig her
                    bgpsATFL = mapVLBGPStatusForATFL(vlBGPeriode);
                    liste.add(bgpsATFL);
                }
            } else {
                BeregningsgrunnlagPrStatus bgps = mapVLBGPStatusForAlleAktivietetStatuser(vlBGPStatus);
                liste.add(bgps);
            }
        }
        return liste;
    }

    // Ikke ATFL og TY, de har separat mapping
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForAlleAktivietetStatuser(BeregningsgrunnlagPrStatusOgAndel statusOgAndel) {
        final AktivitetStatus regelAktivitetStatus = AktivitetStatusMapper.fraVLTilRegel(statusOgAndel.getAktivitetStatus());
        return BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(regelAktivitetStatus)
            .medRedusertBrukersAndelPrÅr(statusOgAndel.getRedusertBrukersAndelPrÅr())
            .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(statusOgAndel.getInntektskategori()))
            .build();
    }

    // Felles mapping av alle statuser som mapper til ATFL
    private static BeregningsgrunnlagPrStatus mapVLBGPStatusForATFL(no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode vlBGPeriode) {

        BeregningsgrunnlagPrStatus.Builder regelBGPStatusATFL = BeregningsgrunnlagPrStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ATFL);

        for (var status : vlBGPeriode.getBeregningsgrunnlagPrStatusOgAndelList()) {
            if (AktivitetStatus.ATFL.equals(AktivitetStatusMapper.fraVLTilRegel(status.getAktivitetStatus()))) {
                BeregningsgrunnlagPrArbeidsforhold regelArbeidsforhold = BeregningsgrunnlagPrArbeidsforhold.builder()
                    .medArbeidsforhold(ArbeidsforholdMapper.mapArbeidsforholdFraBeregningsgrunnlag(status))
                    .medRedusertRefusjonPrÅr(status.getRedusertRefusjonPrÅr())
                    .medRedusertBrukersAndelPrÅr(status.getRedusertBrukersAndelPrÅr())
                    .medInntektskategori(InntektskategoriMapper.fraVLTilRegel(status.getInntektskategori()))
                    .build();
                regelBGPStatusATFL.medArbeidsforhold(regelArbeidsforhold);
            }
        }
        return regelBGPStatusATFL.build();
    }
}
