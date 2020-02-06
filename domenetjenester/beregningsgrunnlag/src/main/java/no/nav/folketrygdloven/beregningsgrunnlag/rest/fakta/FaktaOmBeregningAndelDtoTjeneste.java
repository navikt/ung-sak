package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KontrollerFaktaBeregningFrilanserTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KontrollerFaktaBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.LønnsendringTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.ATogFLISammeOrganisasjonDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningAndelDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

// TODO (Safir) Denne bør splittes opp til tre klasser: ei klasse for ATFL i samme org, ei for frilanser og ei for AT med lønnsendring (uten inntektsmelding)
@ApplicationScoped
public class FaktaOmBeregningAndelDtoTjeneste {

    private BeregningsgrunnlagDtoUtil dtoUtil;

    FaktaOmBeregningAndelDtoTjeneste() {
        // Hibernate
    }

    @Inject
    public FaktaOmBeregningAndelDtoTjeneste(BeregningsgrunnlagDtoUtil dtoUtil) {
        this.dtoUtil = dtoUtil;
    }

    Optional<FaktaOmBeregningAndelDto> lagFrilansAndelDto(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        if (beregningsgrunnlag.getBeregningsgrunnlagPerioder().isEmpty()) {
            return Optional.empty();
        }
        BeregningsgrunnlagPeriode førstePeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        BeregningsgrunnlagPrStatusOgAndel frilansAndel = førstePeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.FRILANSER))
            .findFirst()
            .orElse(null);
        if (frilansAndel != null) {
            FaktaOmBeregningAndelDto dto = new FaktaOmBeregningAndelDto();
            dtoUtil.lagArbeidsforholdDto(frilansAndel, Optional.empty(), inntektArbeidYtelseGrunnlag)
                .ifPresent(dto::setArbeidsforhold);
            dto.setInntektskategori(frilansAndel.getInntektskategori());
            dto.setAndelsnr(frilansAndel.getAndelsnr());
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    /// ATFL I samme organisasjon
    List<ATogFLISammeOrganisasjonDto> lagATogFLISAmmeOrganisasjonListe(BehandlingReferanse behandlingReferanse,
                                                                       BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                       Collection<Inntektsmelding> inntektsmeldinger,
                                                                       InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        Set<Arbeidsgiver> arbeidsgivere = KontrollerFaktaBeregningFrilanserTjeneste
            .brukerErArbeidstakerOgFrilanserISammeOrganisasjon(behandlingReferanse.getAktørId(), beregningsgrunnlag, inntektArbeidYtelseGrunnlag);
        if (arbeidsgivere.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> arbeidsgivereSomErVirksomheter = arbeidsgivere
            .stream()
            .filter(Arbeidsgiver::getErVirksomhet)
            .map(Arbeidsgiver::getOrgnr)
            .collect(Collectors.toSet());

        Map<String, List<Inntektsmelding>> inntektsmeldingMap = KontrollerFaktaBeregningTjeneste
            .hentInntektsmeldingerForVirksomheter(arbeidsgivereSomErVirksomheter, inntektsmeldinger);
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getAktivitetStatus().erArbeidstaker())
            .collect(Collectors.toList());

        List<ATogFLISammeOrganisasjonDto> resultatListe = new ArrayList<>();
        for (Arbeidsgiver arbeidsgiver : arbeidsgivere) {
            andeler.stream()
                .filter(
                    andel -> andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).map(a -> a.equals(arbeidsgiver)).orElse(false))
                .forEach(andel -> resultatListe.add(lagATogFLISAmmeOrganisasjon(andel, inntektsmeldingMap, inntektArbeidYtelseGrunnlag)));
        }
        return resultatListe;
    }

    private ATogFLISammeOrganisasjonDto lagATogFLISAmmeOrganisasjon(BeregningsgrunnlagPrStatusOgAndel andel,
                                                                    Map<String, List<Inntektsmelding>> inntektsmeldingMap,
                                                                    InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        ATogFLISammeOrganisasjonDto dto = new ATogFLISammeOrganisasjonDto();
        dtoUtil.lagArbeidsforholdDto(andel, Optional.empty(), inntektArbeidYtelseGrunnlag)
            .ifPresent(dto::setArbeidsforhold);
        dto.setAndelsnr(andel.getAndelsnr());
        dto.setInntektskategori(andel.getInntektskategori());

        // Privapersoner sender ikke inntektsmelding, disse må alltid fastsettes
        if (andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).map(Arbeidsgiver::getErVirksomhet).orElse(false)) {
            Optional<Inntektsmelding> inntektsmelding = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver)
                .filter(Arbeidsgiver::getErVirksomhet)
                .flatMap(arbeidsgiver -> finnRiktigInntektsmelding(
                    inntektsmeldingMap,
                    arbeidsgiver.getOrgnr(),
                    andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef)));
            inntektsmelding.ifPresent(im -> dto.setInntektPrMnd(im.getInntektBeløp().getVerdi()));
        }
        return dto;
    }

    /// Arbeidsforhold uten inntektsmelding
    List<FaktaOmBeregningAndelDto> lagArbeidsforholdUtenInntektsmeldingDtoList(AktørId aktørId,
                                                                               BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        List<Yrkesaktivitet> aktiviteterMedLønnsendring = LønnsendringTjeneste.finnAlleAktiviteterMedLønnsendringUtenInntektsmelding(aktørId,
            beregningsgrunnlag, inntektArbeidYtelseGrunnlag);
        if (aktiviteterMedLønnsendring.isEmpty()) {
            return Collections.emptyList();
        }
        List<BeregningsgrunnlagPrStatusOgAndel> andeler = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList()
            .stream()
            .filter(andel -> andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver).filter(Arbeidsgiver::getErVirksomhet).isPresent())
            .collect(Collectors.toList());
        List<FaktaOmBeregningAndelDto> arbeidsforholdMedLønnsendringUtenIMDtoList = new ArrayList<>();
        for (Yrkesaktivitet yrkesaktivitet : aktiviteterMedLønnsendring) {
            BeregningsgrunnlagPrStatusOgAndel korrektAndel = finnKorrektAndelFraArbeidsgiver(andeler, yrkesaktivitet.getArbeidsgiver());
            FaktaOmBeregningAndelDto dto = lagArbeidsforholdUtenInntektsmeldingDto(korrektAndel, inntektArbeidYtelseGrunnlag);
            arbeidsforholdMedLønnsendringUtenIMDtoList.add(dto);
        }
        return arbeidsforholdMedLønnsendringUtenIMDtoList;
    }

    private FaktaOmBeregningAndelDto lagArbeidsforholdUtenInntektsmeldingDto(BeregningsgrunnlagPrStatusOgAndel andel, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        FaktaOmBeregningAndelDto dto = new FaktaOmBeregningAndelDto();
        dtoUtil.lagArbeidsforholdDto(andel, Optional.empty(), inntektArbeidYtelseGrunnlag)
            .ifPresent(dto::setArbeidsforhold);
        dto.setAndelsnr(andel.getAndelsnr());
        dto.setInntektskategori(andel.getInntektskategori());
        return dto;
    }

    private BeregningsgrunnlagPrStatusOgAndel finnKorrektAndelFraArbeidsgiver(List<BeregningsgrunnlagPrStatusOgAndel> andeler, Arbeidsgiver arbeidsgiver) {
        return andeler.stream()
            .filter(andel -> andel.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getArbeidsgiver)
                .map(ag -> ag.equals(arbeidsgiver))
                .orElse(false))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Finner ikke korrekt andel for yrkesaktiviteten"));
    }

    private Optional<Inntektsmelding> finnRiktigInntektsmelding(Map<String, List<Inntektsmelding>> inntektsmeldingMap, String virksomhetOrgnr,
                                                                Optional<InternArbeidsforholdRef> arbeidsforholdRef) {
        if (!inntektsmeldingMap.containsKey(virksomhetOrgnr)) {
            return Optional.empty();
        }
        Collection<Inntektsmelding> inntektsmeldinger = inntektsmeldingMap.get(virksomhetOrgnr);
        if (inntektsmeldinger.size() == 1) {
            return Optional.ofNullable(List.copyOf(inntektsmeldinger).get(0));
        }
        return inntektsmeldinger.stream()
            .filter(im -> arbeidsforholdRef.map(ref -> ref.equals(im.getArbeidsforholdRef()))
                .orElse(false))
            .findFirst();
    }

}
