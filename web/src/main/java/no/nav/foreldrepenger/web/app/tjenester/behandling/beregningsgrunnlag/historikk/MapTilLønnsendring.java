package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.DagpengeAndelLagtTilBesteberegningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsettMånedsinntektUtenInntektsmeldingAndelDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller.Lønnsendring;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.Inntektskategori;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelDto;

public class MapTilLønnsendring {

    private static final int MND_I_1_ÅR = 12;

    private MapTilLønnsendring() {
        // Skjul public constructor
    }


    // TODO: Refaktorer og gjer denne lettere å lese
    public static Lønnsendring mapTilLønnsendringForAndelIPeriode(RedigerbarAndelDto andel,
                                                           FastsatteVerdierDto fastsatteVerdier,
                                                           BeregningsgrunnlagPeriode periode,
                                                           Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag) {
        if (andel.getNyAndel() || andel.getLagtTilAvSaksbehandler()) {
            if (andel.getNyAndel() && andel.getAndelsnr() == null && andel.getAktivitetStatus() != null) {
                return new Lønnsendring.Builder()
                    .medNyAndel(true)
                    .medAktivitetStatus(andel.getAktivitetStatus())
                    .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
                    .medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
                    .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue()).build();
            }
            return lagLønnsendringForNyAndelFraAndelsreferanse(andel, periode, periodeForrigeGrunnlag, fastsatteVerdier);
        } else {
            BeregningsgrunnlagPrStatusOgAndel andelIAktivt = finnKorrektAndelIPeriode(periode, andel.getAndelsnr());
            Optional<BeregningsgrunnlagPrStatusOgAndel> andelFraForrige = periodeForrigeGrunnlag.map(p -> finnKorrektAndelIPeriode(p, andel.getAndelsnr()));
            return new Lønnsendring.Builder()
                .medGammelInntektskategori(andelFraForrige.map(BeregningsgrunnlagPrStatusOgAndel::getInntektskategori).orElse(null))
                .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
                .medGammelArbeidsinntekt(andelFraForrige.map(MapTilLønnsendring::finnBeregnetPrMnd).orElse(null))
                .medGammelArbeidsinntektPrÅr(andelFraForrige.map(MapTilLønnsendring::finnBeregnetPrMnd).orElse(null))
                .medGammelRefusjonPrÅr(andelFraForrige.flatMap(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold).map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
                    .map(BigDecimal::intValue).orElse(0))
                .medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
                .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue())
                .medAktivitetStatus(andelIAktivt.getAktivitetStatus())
                .medArbeidsforholdRef(andelIAktivt.getArbeidsforholdRef().orElse(null))
                .medAktivitetStatus(andelIAktivt.getAktivitetStatus())
                .build();
        }
    }

    private static Lønnsendring lagLønnsendringForNyAndelFraAndelsreferanse(RedigerbarAndelDto andel, BeregningsgrunnlagPeriode periode,
                                                                            Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag,
                                                                            FastsatteVerdierDto fastsatteVerdier) {
        Lønnsendring.Builder endringBuilder = new Lønnsendring.Builder();
        if (!andel.getNyAndel() && periodeForrigeGrunnlag.isPresent()) {
            BeregningsgrunnlagPrStatusOgAndel forrigeAndel = finnKorrektAndelIPeriode(periodeForrigeGrunnlag.get(), andel.getAndelsnr());
            endringBuilder.medNyAndel(false)
                .medAktivitetStatus(forrigeAndel.getAktivitetStatus())
                .medArbeidsforholdRef(forrigeAndel.getArbeidsforholdRef().orElse(null))
                .medArbeidsgiver(forrigeAndel.getArbeidsgiver().orElse(null))
                .medGammelArbeidsinntekt(finnBeregnetPrMnd(forrigeAndel))
                .medGammelArbeidsinntektPrÅr(forrigeAndel.getBeregnetPrÅr() == null ? null : forrigeAndel.getBeregnetPrÅr().intValue())
                .medGammelInntektskategori(forrigeAndel.getInntektskategori())
                .medGammelRefusjonPrÅr(forrigeAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
                    .map(BigDecimal::intValue).orElse(0));
        } else {
            BeregningsgrunnlagPrStatusOgAndel andelSomErKopiert = finnKorrektAndelIPeriode(periode, andel.getAndelsnr());
            endringBuilder.medNyAndel(true)
                .medAktivitetStatus(andelSomErKopiert.getAktivitetStatus())
                .medArbeidsgiver(andelSomErKopiert.getArbeidsgiver().orElse(null))
                .medArbeidsforholdRef(andelSomErKopiert.getArbeidsforholdRef().orElse(null))
                .medGammelRefusjonPrÅr(0);
        }
        return endringBuilder.medNyArbeidsinntekt(fastsatteVerdier.getFastsattBeløp())
            .medNyArbeidsinntektPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr().intValue())
            .medNyInntektskategori(fastsatteVerdier.getInntektskategori())
            .medNyRefusjonPrÅr(fastsatteVerdier.getRefusjonPrÅr())
            .build();
    }


    private static Integer finnBeregnetPrMnd(BeregningsgrunnlagPrStatusOgAndel korrektAndel) {
        return korrektAndel.getBeregnetPrÅr() == null ? null : korrektAndel.getBeregnetPrÅr()
            .divide(BigDecimal.valueOf(MND_I_1_ÅR), RoundingMode.HALF_EVEN).intValue();
    }

    public static List<Lønnsendring> mapTilLønnsendringFraBesteberegning(FaktaBeregningLagreDto dto, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        List<Lønnsendring> endringer = dto.getBesteberegningAndeler().getBesteberegningAndelListe().stream()
            .filter(a -> !a.getNyAndel())
            .map(dtoAndel -> mapTilLønnsendring(
                dtoAndel.getAndelsnr(),
                dtoAndel.getFastsatteVerdier().getFastsattBeløp(),
                nyttBeregningsgrunnlag,
                forrigeBg))
            .collect(Collectors.toList());
        DagpengeAndelLagtTilBesteberegningDto nyDagpengeAndel = dto.getBesteberegningAndeler().getNyDagpengeAndel();
        if (nyDagpengeAndel != null) {
            Lønnsendring lønnsendringForNyAndel = mapTilLønnsendring(
                AktivitetStatus.DAGPENGER,
                nyDagpengeAndel.getFastsatteVerdier().getFastsattBeløp(),
                nyttBeregningsgrunnlag,
                forrigeBg);
            endringer.add(lønnsendringForNyAndel);
        }
        return endringer;
    }

    public static List<Lønnsendring> mapLønnsendringFraATogFLSammeOrg(FaktaBeregningLagreDto dto, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return dto.getVurderATogFLiSammeOrganisasjon().getVurderATogFLiSammeOrganisasjonAndelListe().stream()
            .map(a -> mapTilLønnsendring(a.getAndelsnr(), a.getArbeidsinntekt(), nyttBeregningsgrunnlag, forrigeBg))
        .collect(Collectors.toList());
    }

    public static List<Lønnsendring> mapLønnsendringFraATUtenInntektsmelding(FaktaBeregningLagreDto dto, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        return dto.getFastsattUtenInntektsmelding().getAndelListe().stream()
            .map(dtoAndel -> mapTilLønnsendring(dtoAndel.getAndelsnr(), mapTilFastsatteVerdier(dtoAndel), nyttBeregningsgrunnlag, forrigeBg))
        .collect(Collectors.toList());
    }

    private static FastsatteVerdierDto mapTilFastsatteVerdier(FastsettMånedsinntektUtenInntektsmeldingAndelDto dtoAndel) {
        return new FastsatteVerdierDto(dtoAndel.getFastsattBeløp());
    }

    private static Lønnsendring mapTilLønnsendring(Long andelsnr,
                                                   Integer nyArbeidsinntekt,
                                                   BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                   Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        BeregningsgrunnlagPrStatusOgAndel andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, andelsnr);
        Integer forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, andelsnr);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, nyArbeidsinntekt);
    }

    private static Lønnsendring mapTilLønnsendring(Long andelsnr,
                                                   FastsatteVerdierDto fastsatteVerdierDto,
                                                   BeregningsgrunnlagEntitet nyttBeregningsgrunnlag,
                                                   Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        BeregningsgrunnlagPrStatusOgAndel andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, andelsnr);
        Integer forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, andelsnr);
        Inntektskategori forrigeInntektskategori = finnGammelInntektskategori(forrigeBg, andelsnr);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, forrigeInntektskategori, fastsatteVerdierDto);
    }

    public static Lønnsendring mapTilLønnsendring(AktivitetStatus aktivitetStatus, Integer nyArbeidsinntekt, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        if (aktivitetStatus.equals(AktivitetStatus.ARBEIDSTAKER)) {
            throw new IllegalArgumentException("Utviklerfeil: Kan ikke sette lønnsendring basert på status med flere andeler. Bruk andelsnr.");
        }
        BeregningsgrunnlagPrStatusOgAndel andel = finnKorrektAndelIFørstePeriode(nyttBeregningsgrunnlag, aktivitetStatus)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke andel for aktivitetStatus " + aktivitetStatus));
        Integer forrigeInntekt = finnGammelMånedsinntekt(forrigeBg, aktivitetStatus);
        return mapAndelTilLønnsendring(andel, forrigeInntekt, nyArbeidsinntekt);
    }

    private static Integer finnGammelMånedsinntekt(Optional<BeregningsgrunnlagEntitet> forrigeBg, Long andelsnr) {
        return forrigeBg
                .map(bg -> finnKorrektAndelIFørstePeriode(bg, andelsnr))
                .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
                .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
                .map(MapTilLønnsendring::mapTilMånedsbeløp)
                .orElse(null);
    }

    private static Integer finnGammelMånedsinntekt(Optional<BeregningsgrunnlagEntitet> forrigeBg, AktivitetStatus aktivitetStatus) {
        return forrigeBg
            .flatMap(bg -> finnKorrektAndelIFørstePeriode(bg, aktivitetStatus))
            .stream()
            .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .findFirst()
            .map(BeregningsgrunnlagPrStatusOgAndel::getBeregnetPrÅr)
            .map(MapTilLønnsendring::mapTilMånedsbeløp)
            .orElse(null);
    }

    private static Inntektskategori finnGammelInntektskategori(Optional<BeregningsgrunnlagEntitet> forrigeBg, Long andelsnr) {
        return forrigeBg
            .map(bg -> finnKorrektAndelIFørstePeriode(bg, andelsnr))
            .filter(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .map(BeregningsgrunnlagPrStatusOgAndel::getInntektskategori)
            .orElse(null);
    }

    private static Lønnsendring mapAndelTilLønnsendring(BeregningsgrunnlagPrStatusOgAndel andel,
                                                        Integer forrigeInntekt,
                                                        Inntektskategori forrigeInntektskategori,
                                                        FastsatteVerdierDto fastsatteVerdierDto) {
        return Lønnsendring.Builder.ny()
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef().orElse(null))
            .medNyArbeidsinntekt(fastsatteVerdierDto.getFastsattBeløp())
            .medGammelArbeidsinntekt(forrigeInntekt)
            .medNyInntektskategori(fastsatteVerdierDto.getInntektskategori())
            .medGammelInntektskategori(forrigeInntektskategori)
            .build();
    }

    private static Lønnsendring mapAndelTilLønnsendring(BeregningsgrunnlagPrStatusOgAndel andel, Integer forrigeInntekt, Integer nyArbeidsinntekt) {
        return Lønnsendring.Builder.ny()
            .medAktivitetStatus(andel.getAktivitetStatus())
            .medArbeidsgiver(andel.getArbeidsgiver().orElse(null))
            .medArbeidsforholdRef(andel.getArbeidsforholdRef().orElse(null))
            .medNyArbeidsinntekt(nyArbeidsinntekt)
            .medGammelArbeidsinntekt(forrigeInntekt)
            .build();
    }

    private static Integer mapTilMånedsbeløp(BigDecimal beregnet) {
        if (beregnet == null) {
            return null;
        }
        return beregnet.intValue() / MND_I_1_ÅR;
    }

    private static BeregningsgrunnlagPrStatusOgAndel finnKorrektAndelIPeriode(BeregningsgrunnlagPeriode periode, Long andelsnr) {
        return periode
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bgAndel -> andelsnr.equals(bgAndel.getAndelsnr()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke andel for andelsnr " + andelsnr));
    }

    private static BeregningsgrunnlagPrStatusOgAndel finnKorrektAndelIFørstePeriode(BeregningsgrunnlagEntitet bg, Long andelsnr) {
        return finnKorrektAndelIPeriode(bg.getBeregningsgrunnlagPerioder().get(0), andelsnr);
    }

    private static Optional<BeregningsgrunnlagPrStatusOgAndel> finnKorrektAndelIFørstePeriode(BeregningsgrunnlagEntitet bg, AktivitetStatus aktivitetStatus) {
        return bg.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(bgAndel -> aktivitetStatus.equals(bgAndel.getAktivitetStatus()))
            .findFirst();
    }
}
