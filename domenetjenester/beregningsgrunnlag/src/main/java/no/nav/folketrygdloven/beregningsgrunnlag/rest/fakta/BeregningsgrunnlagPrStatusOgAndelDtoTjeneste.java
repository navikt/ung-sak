package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static java.util.stream.Collectors.toList;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.SammenligningsgrunnlagPrStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagPrStatusOgAndelDto;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.SammenligningsgrunnlagType;


@ApplicationScoped
public class BeregningsgrunnlagPrStatusOgAndelDtoTjeneste {

    private static final int MND_I_ÅR = 12;
    private BeregningsgrunnlagDtoUtil dtoUtil;
    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;
    private static final Map<SammenligningsgrunnlagType, AktivitetStatus> SAMMENLIGNINGSGRUNNLAGTYPE_AKTIVITETSTATUS_MAP;
    static {
        SAMMENLIGNINGSGRUNNLAGTYPE_AKTIVITETSTATUS_MAP = Map.of(
            SammenligningsgrunnlagType.SAMMENLIGNING_AT, AktivitetStatus.ARBEIDSTAKER,
            SammenligningsgrunnlagType.SAMMENLIGNING_FL, AktivitetStatus.FRILANSER,
            SammenligningsgrunnlagType.SAMMENLIGNING_SN, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
    }

    BeregningsgrunnlagPrStatusOgAndelDtoTjeneste() {
        // Hibernate
    }


    @Inject
    public BeregningsgrunnlagPrStatusOgAndelDtoTjeneste(BeregningsgrunnlagDtoUtil dtoUtil,
                                                        FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste) {
        this.dtoUtil = dtoUtil;
        this.refusjonOgGraderingTjeneste = refusjonOgGraderingTjeneste;
    }

    List<BeregningsgrunnlagPrStatusOgAndelDto> lagBeregningsgrunnlagPrStatusOgAndelDto(BeregningsgrunnlagInput input,
                                                                                       List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndelList,
                                                                                       List<BeregningsgrunnlagPrStatusOgAndel> faktaAndelListe,
                                                                                       List<BeregningsgrunnlagPrStatusOgAndel> overstyrtAndelListe) {

        List<BeregningsgrunnlagPrStatusOgAndelDto> usortertDtoList = new ArrayList<>();
        for (BeregningsgrunnlagPrStatusOgAndel andel : beregningsgrunnlagPrStatusOgAndelList) {
            Optional<BeregningsgrunnlagPrStatusOgAndel> faktaAndel = finnAndel(faktaAndelListe, andel);
            Optional<BeregningsgrunnlagPrStatusOgAndel> overstyrtAndel = finnAndel(overstyrtAndelListe, andel);
            BeregningsgrunnlagPrStatusOgAndelDto dto = lagDto(input, andel, faktaAndel, overstyrtAndel);
            usortertDtoList.add(dto);
        }
        // Følgende gjøres for å sortere arbeidsforholdene etter beregnet årsinntekt og deretter arbedsforholdId
        List<BeregningsgrunnlagPrStatusOgAndelDto> arbeidsarbeidstakerAndeler = usortertDtoList.stream().filter(dto -> dto.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).collect(toList());
        List<BeregningsgrunnlagPrStatusOgAndelDto> alleAndreAndeler = usortertDtoList.stream().filter(dto -> !dto.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER)).collect(toList());
        if (dtoKanSorteres(arbeidsarbeidstakerAndeler)) {
            arbeidsarbeidstakerAndeler.sort(comparatorEtterBeregnetOgArbeidsforholdId());
        }
        List<BeregningsgrunnlagPrStatusOgAndelDto> dtoList = new ArrayList<>(arbeidsarbeidstakerAndeler);
        dtoList.addAll(alleAndreAndeler);

        return dtoList;
    }

    private BeregningsgrunnlagPrStatusOgAndelDto lagDto(BeregningsgrunnlagInput input,
                                                        BeregningsgrunnlagPrStatusOgAndel andel,
                                                        Optional<BeregningsgrunnlagPrStatusOgAndel> faktaAndel,
                                                        Optional<BeregningsgrunnlagPrStatusOgAndel> overstyrtAndel) {
        var iayGrunnlag = input.getIayGrunnlag();
        var inntektsmeldinger = input.getInntektsmeldinger();
        var ref = input.getBehandlingReferanse();
        var beregningAktivitetAggregat = input.getBeregningsgrunnlagGrunnlag().getGjeldendeAktiviteter();
        BeregningsgrunnlagPrStatusOgAndelDto dto = LagTilpassetDtoTjeneste.opprettTilpassetDTO(ref, andel, faktaAndel, iayGrunnlag);
        Optional<Inntektsmelding> inntektsmelding = BeregningInntektsmeldingTjeneste.finnInntektsmeldingForAndel(andel, inntektsmeldinger);
        dtoUtil.lagArbeidsforholdDto(andel, inntektsmelding, iayGrunnlag).ifPresent(dto::setArbeidsforhold);
        dto.setDagsats(andel.getDagsats());
        dto.setOriginalDagsatsFraTilstøtendeYtelse(andel.getOrginalDagsatsFraTilstøtendeYtelse());
        dto.setAndelsnr(andel.getAndelsnr());
        dto.setAktivitetStatus(andel.getAktivitetStatus());
        dto.setBeregningsperiodeFom(andel.getBeregningsperiodeFom());
        dto.setBeregningsperiodeTom(andel.getBeregningsperiodeTom());
        settVerdierFraAndreGrunnlag(andel, faktaAndel, overstyrtAndel, dto);
        dto.setBruttoPrAar(andel.getBruttoPrÅr());
        dto.setFordeltPrAar(andel.getFordeltPrÅr());
        dto.setAvkortetPrAar(andel.getAvkortetPrÅr());
        dto.setRedusertPrAar(andel.getRedusertPrÅr());
        dto.setErTidsbegrensetArbeidsforhold(andel.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold).orElse(null));
        dto.setErNyIArbeidslivet(andel.getNyIArbeidslivet());
        faktaAndel.map(BeregningsgrunnlagPrStatusOgAndel::getFastsattAvSaksbehandler)
            .ifPresentOrElse(dto::setFastsattAvSaksbehandler,
                () -> dto.setFastsattAvSaksbehandler(andel.getFastsattAvSaksbehandler()));
        dto.setLonnsendringIBeregningsperioden(andel.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden).orElse(null));
        dto.setLagtTilAvSaksbehandler(andel.getLagtTilAvSaksbehandler());
        dto.setErTilkommetAndel(refusjonOgGraderingTjeneste.erNyttArbeidsforhold(andel, beregningAktivitetAggregat));
        dto.setSkalFastsetteGrunnlag(skalGrunnlagFastsettes(input, andel));
        LocalDate skjæringstidspunkt = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
        if (andel.getAktivitetStatus().erArbeidstaker()) {
            iayGrunnlag.getAktørInntektFraRegister(ref.getAktørId())
                .ifPresent(aktørInntekt -> {
                    var filter = new InntektFilter(aktørInntekt).før(skjæringstidspunkt);
                    BigDecimal årsbeløp = InntektForAndelTjeneste
                        .finnSnittinntektPrÅrForArbeidstakerIBeregningsperioden(filter, andel);
                    BigDecimal månedsbeløp = årsbeløp.divide(BigDecimal.valueOf(MND_I_ÅR), 10, RoundingMode.HALF_EVEN);
                    dto.setBelopPrMndEtterAOrdningen(månedsbeløp);
                    dto.setBelopPrAarEtterAOrdningen(årsbeløp);
                });
        } else if (andel.getAktivitetStatus().erFrilanser()) {
            InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(ref.getAktørId(),
                    iayGrunnlag, andel, skjæringstidspunkt)
                .ifPresent(dto::setBelopPrMndEtterAOrdningen);
        }
        return dto;
    }

    private boolean skalGrunnlagFastsettes(BeregningsgrunnlagInput input, BeregningsgrunnlagPrStatusOgAndel andel){
        Optional<SammenligningsgrunnlagPrStatus> sammenligningsgrunnlagSomIkkeErSplittet = input.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().stream()
            .filter(s -> s.getSammenligningsgrunnlagType().equals(SammenligningsgrunnlagType.SAMMENLIGNING_ATFL_SN))
            .findAny();

        if (erAndelForArbeidstakerEllerFrilanser(andel) && sammenligningsgrunnlagSomIkkeErSplittet.isPresent()){
            return BigDecimal.valueOf(sammenligningsgrunnlagSomIkkeErSplittet.get().getAvvikPromille()).compareTo(BigDecimal.valueOf(250)) > 0;
        }

        return input.getBeregningsgrunnlag().getSammenligningsgrunnlagPrStatusListe().stream()
            .filter(s -> SAMMENLIGNINGSGRUNNLAGTYPE_AKTIVITETSTATUS_MAP.get(s.getSammenligningsgrunnlagType()).equals(andel.getAktivitetStatus()))
            .anyMatch(s -> BigDecimal.valueOf(s.getAvvikPromille()).compareTo(BigDecimal.valueOf(250)) > 0);
    }

    private boolean erAndelForArbeidstakerEllerFrilanser(BeregningsgrunnlagPrStatusOgAndel andel){
        return andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER) || andel.getAktivitetStatus().equals(AktivitetStatus.FRILANSER);
    }

    private void settVerdierFraAndreGrunnlag(BeregningsgrunnlagPrStatusOgAndel andel,
                                             Optional<BeregningsgrunnlagPrStatusOgAndel> faktaAndel,
                                             Optional<BeregningsgrunnlagPrStatusOgAndel> overstyrtAndel,
                                             BeregningsgrunnlagPrStatusOgAndelDto dto) {
        faktaAndel.ifPresentOrElse(a -> settHvisFastsattAvSaksbehandler(andel, dto, a), () -> settRedigerbareVerdierIFaktaOmBeregning(andel, dto));
        overstyrtAndel.ifPresentOrElse(a -> settHvisOverstyrt(andel, dto, a), () -> dto.setOverstyrtPrAar(andel.getOverstyrtPrÅr()));
    }


    private void settHvisOverstyrt(BeregningsgrunnlagPrStatusOgAndel andel, BeregningsgrunnlagPrStatusOgAndelDto dto, BeregningsgrunnlagPrStatusOgAndel a) {
        if (a.getOverstyrtPrÅr() != null) {
            dto.setOverstyrtPrAar(a.getOverstyrtPrÅr());
        } else {
            dto.setOverstyrtPrAar(andel.getOverstyrtPrÅr());
        }
    }

    private void settHvisFastsattAvSaksbehandler(BeregningsgrunnlagPrStatusOgAndel andel, BeregningsgrunnlagPrStatusOgAndelDto dto, BeregningsgrunnlagPrStatusOgAndel a) {
        if (Boolean.TRUE.equals(a.getFastsattAvSaksbehandler())) {
            settRedigerbareVerdierIFaktaOmBeregning(a, dto);
        } else {
            settRedigerbareVerdierIFaktaOmBeregning(andel, dto);
        }
    }

    private void settRedigerbareVerdierIFaktaOmBeregning(BeregningsgrunnlagPrStatusOgAndel andel, BeregningsgrunnlagPrStatusOgAndelDto dto) {
        dto.setInntektskategori(andel.getInntektskategori());
        dto.setBeregnetPrAar(andel.getBeregnetPrÅr());
        dto.setBesteberegningPrAar(andel.getBesteberegningPrÅr());
    }

    private Optional<BeregningsgrunnlagPrStatusOgAndel> finnAndel(List<BeregningsgrunnlagPrStatusOgAndel> sisteGrunnlagAndelList, BeregningsgrunnlagPrStatusOgAndel andel) {
        return sisteGrunnlagAndelList.stream()
            .filter(a -> a.equals(andel)).findFirst();
    }

    private boolean dtoKanSorteres(List<BeregningsgrunnlagPrStatusOgAndelDto> arbeidsarbeidstakerAndeler) {
        List<BeregningsgrunnlagPrStatusOgAndelDto> listMedNull = arbeidsarbeidstakerAndeler
            .stream()
            .filter(a -> a.getBeregnetPrAar() == null)
            .collect(toList());
        return listMedNull.isEmpty();
    }

    private Comparator<BeregningsgrunnlagPrStatusOgAndelDto> comparatorEtterBeregnetOgArbeidsforholdId() {
        return Comparator.comparing(BeregningsgrunnlagPrStatusOgAndelDto::getBeregnetPrAar)
            .reversed();
    }

}
