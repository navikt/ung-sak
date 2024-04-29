package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregat;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyring;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.kalkulus.felles.v1.Beløp;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPeriodeDto;
import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAktivitetHandlingType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class FraKalkulusMapper {

    public static BeregningsgrunnlagGrunnlag mapBeregningsgrunnlagGrunnlag(BeregningsgrunnlagGrunnlagDto grunnlagDto) {
        return BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag() == null ? null : mapBeregningsgrunnlag(grunnlagDto.getBeregningsgrunnlag()))
            .medRegisterAktiviteter(mapBeregningAktivitetAggregat(grunnlagDto.getRegisterAktiviteter()))
            .medSaksbehandletAktiviteter(grunnlagDto.getSaksbehandletAktiviteter() == null ? null : mapBeregningAktivitetAggregat(grunnlagDto.getSaksbehandletAktiviteter()))
            .medOverstyring(grunnlagDto.getOverstyringer() == null ? null : mapBeregningAktivitetOverstyringer(grunnlagDto.getOverstyringer()))
            .build(BeregningsgrunnlagTilstand.fraKode(grunnlagDto.getBeregningsgrunnlagTilstand().getKode()));
    }

    private static BeregningAktivitetOverstyringer mapBeregningAktivitetOverstyringer(BeregningAktivitetOverstyringerDto overstyringer) {
        BeregningAktivitetOverstyringer.Builder builder = BeregningAktivitetOverstyringer.builder();
        overstyringer.getOverstyringer().stream().map(FraKalkulusMapper::mapAktivitetOverstyring).forEach(builder::leggTilOverstyring);
        return builder.build();
    }

    private static BeregningAktivitetOverstyring mapAktivitetOverstyring(BeregningAktivitetOverstyringDto beregningAktivitetOverstyringDto) {
        return BeregningAktivitetOverstyring.builder()
            .medArbeidsgiver(beregningAktivitetOverstyringDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(beregningAktivitetOverstyringDto.getArbeidsgiver()))
            .medArbeidsforholdRef(mapArbeidsforholdRef(beregningAktivitetOverstyringDto.getArbeidsforholdRef()))
            .medHandling(BeregningAktivitetHandlingType.fraKode(beregningAktivitetOverstyringDto.getHandlingType().getKode()))
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetOverstyringDto.getPeriode().getFom(), beregningAktivitetOverstyringDto.getPeriode().getTom()))
            .build();
    }

    private static InternArbeidsforholdRef mapArbeidsforholdRef(InternArbeidsforholdRefDto arbeidsforholdRef) {
        if (arbeidsforholdRef == null) {
            return InternArbeidsforholdRef.nullRef();
        }
        return InternArbeidsforholdRef.ref(arbeidsforholdRef.getAbakusReferanse());
    }

    private static BeregningAktivitetAggregat mapBeregningAktivitetAggregat(BeregningAktivitetAggregatDto registerAktiviteter) {
        BeregningAktivitetAggregat.Builder builder = BeregningAktivitetAggregat.builder()
            .medSkjæringstidspunktOpptjening(registerAktiviteter.getSkjæringstidspunktOpptjening());
        registerAktiviteter.getAktiviteter().stream()
            .map(FraKalkulusMapper::mapBeregningAktivitet)
            .forEach(builder::leggTilAktivitet);
        return builder
            .build();
    }

    private static BeregningAktivitet mapBeregningAktivitet(BeregningAktivitetDto beregningAktivitetDto) {
        return BeregningAktivitet.builder()
            .medArbeidsforholdRef(mapArbeidsforholdRef(beregningAktivitetDto.getArbeidsforholdRef()))
            .medArbeidsgiver(beregningAktivitetDto.getArbeidsgiver() == null ? null : mapArbeidsgiver(beregningAktivitetDto.getArbeidsgiver()))
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.fraKode(beregningAktivitetDto.getOpptjeningAktivitetType().getKode()))
            .medPeriode(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(beregningAktivitetDto.getPeriode().getFom(), beregningAktivitetDto.getPeriode().getTom()))
            .build();
    }

    public static Beregningsgrunnlag mapBeregningsgrunnlag(BeregningsgrunnlagDto beregningsgrunnlagDto) {
        var builder = Beregningsgrunnlag.builder()
            .medOverstyring(beregningsgrunnlagDto.isOverstyrt())
            .medSkjæringstidspunkt(beregningsgrunnlagDto.getSkjæringstidspunkt())
            .medGrunnbeløp(Beløp.safeVerdi(beregningsgrunnlagDto.getGrunnbeløp()));


        beregningsgrunnlagDto.getAktivitetStatuser().forEach(aktivitetStatus -> {
            builder.leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(AktivitetStatus.fraKode(aktivitetStatus.getKode())));
        });
        if (beregningsgrunnlagDto.getFaktaOmBeregningTilfeller() != null) {
            List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlagDto.getFaktaOmBeregningTilfeller().stream()
                .map(t -> FaktaOmBeregningTilfelle.fraKode(t.getKode()))
                .collect(Collectors.toList());
            builder.leggTilFaktaOmBeregningTilfeller(tilfeller);
        }

        Beregningsgrunnlag bg = builder.build();

        mapPerioder(beregningsgrunnlagDto.getBeregningsgrunnlagPerioder())
            .forEach(periodeBuilder -> periodeBuilder.build(bg));

        return bg;
    }

    private static List<BeregningsgrunnlagPeriode.Builder> mapPerioder(List<BeregningsgrunnlagPeriodeDto> beregningsgrunnlagPerioder) {
        return beregningsgrunnlagPerioder.stream()
            .map(FraKalkulusMapper::mapPeriode)
            .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPeriode.Builder mapPeriode(BeregningsgrunnlagPeriodeDto beregningsgrunnlagPeriodeDto) {
        BeregningsgrunnlagPeriode.Builder periodeBuilder = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeFom(), beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPeriodeTom())
            .medAvkortetPrÅr(Beløp.safeVerdi(beregningsgrunnlagPeriodeDto.getAvkortetPrÅr()))
            .medBruttoPrÅr(Beløp.safeVerdi(beregningsgrunnlagPeriodeDto.getBruttoPrÅr()))
            .medRedusertPrÅr(Beløp.safeVerdi(beregningsgrunnlagPeriodeDto.getRedusertPrÅr()))
            .medTotalUtbetalingsgradFraUttak(beregningsgrunnlagPeriodeDto.getTotalUtbetalingsgradFraUttak())
            .medTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt(beregningsgrunnlagPeriodeDto.getTotalUtbetalingsgradEtterReduksjonVedTilkommetInntekt())
            .medReduksjonsfaktorInaktivTypeA(beregningsgrunnlagPeriodeDto.getReduksjonsfaktorInaktivTypeA());
        mapAndeler(beregningsgrunnlagPeriodeDto.getBeregningsgrunnlagPrStatusOgAndelList()).forEach(periodeBuilder::leggTilBeregningsgrunnlagPrStatusOgAndel);
        return periodeBuilder;
    }

    private static List<BeregningsgrunnlagPrStatusOgAndel.Builder> mapAndeler(List<BeregningsgrunnlagPrStatusOgAndelDto> beregningsgrunnlagPrStatusOgAndelList) {
        return beregningsgrunnlagPrStatusOgAndelList.stream()
            .map(FraKalkulusMapper::mapAndel)
            .collect(Collectors.toList());
    }

    private static BeregningsgrunnlagPrStatusOgAndel.Builder mapAndel(BeregningsgrunnlagPrStatusOgAndelDto beregningsgrunnlagPrStatusOgAndelDto) {
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(AktivitetStatus.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getAktivitetStatus().getKode()))
            .medAndelsnr(beregningsgrunnlagPrStatusOgAndelDto.getAndelsnr())
            .medArbforholdType(OpptjeningAktivitetType.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getArbeidsforholdType().getKode()))
            .medAvkortetBrukersAndelPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetBrukersAndelPrÅr()))
            .medAvkortetPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetPrÅr()))
            .medAvkortetRefusjonPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getAvkortetRefusjonPrÅr()))
            .medBeregnetPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getBeregnetPrÅr()))
            .medBeregningsperiode(beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeFom(), beregningsgrunnlagPrStatusOgAndelDto.getBeregningsperiodeTom())
            .medFastsattAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getFastsattAvSaksbehandler())
            .medFordeltPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getFordeltPrÅr()))
            .medInntektskategori(Inntektskategori.fraKode(beregningsgrunnlagPrStatusOgAndelDto.getInntektskategori().getKode()))
            .medLagtTilAvSaksbehandler(beregningsgrunnlagPrStatusOgAndelDto.getLagtTilAvSaksbehandler())
            .medMaksimalRefusjonPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getMaksimalRefusjonPrÅr()))
            .medOrginalDagsatsFraTilstøtendeYtelse(beregningsgrunnlagPrStatusOgAndelDto.getOrginalDagsatsFraTilstøtendeYtelse())
            .medOverstyrtPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getOverstyrtPrÅr()))
            .medRedusertBrukersAndelPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getRedusertBrukersAndelPrÅr()))
            .medRedusertPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getRedusertPrÅr()))
            .medRedusertRefusjonPrÅr(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getRedusertRefusjonPrÅr()))
            .medÅrsbeløpFraTilstøtendeYtelse(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getÅrsbeløpFraTilstøtendeYtelse()));

        if (beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold() != null) {
            builder.medBGAndelArbeidsforhold(FraKalkulusMapper.mapBgAndelArbeidsforhold(beregningsgrunnlagPrStatusOgAndelDto.getBgAndelArbeidsforhold()));
        }

        if (beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt() != null) {
            builder.medPgi(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getPgiSnitt()),
                List.of(Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getPgi1()),
                    Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getPgi2()),
                    Beløp.safeVerdi(beregningsgrunnlagPrStatusOgAndelDto.getPgi3())));
        }
        return builder;
    }

    private static no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold.Builder mapBgAndelArbeidsforhold(BGAndelArbeidsforhold bgAndelArbeidsforhold) {
        return no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold.builder()
            .medArbeidsforholdRef(Optional.ofNullable(bgAndelArbeidsforhold.getArbeidsforholdRef()).map(UUID::toString).orElse(null))
            .medArbeidsgiver(mapArbeidsgiver(bgAndelArbeidsforhold.getArbeidsgiver()))
            .medArbeidsperiodeFom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medArbeidsperiodeTom(bgAndelArbeidsforhold.getArbeidsperiodeFom())
            .medNaturalytelseBortfaltPrÅr(Beløp.safeVerdi(bgAndelArbeidsforhold.getNaturalytelseBortfaltPrÅr()))
            .medNaturalytelseTilkommetPrÅr(Beløp.safeVerdi(bgAndelArbeidsforhold.getNaturalytelseTilkommetPrÅr()));
    }

    public static Arbeidsgiver mapArbeidsgiver(no.nav.folketrygdloven.kalkulus.response.v1.Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver.getArbeidsgiverOrgnr() != null) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getArbeidsgiverOrgnr());
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiver.getArbeidsgiverAktørId()));
    }
}
