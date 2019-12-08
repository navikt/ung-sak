package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import static no.nav.vedtak.konfig.Tid.TIDENES_BEGYNNELSE;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.GraderingEllerRefusjonDto;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;

@ApplicationScoped
public class RefusjonEllerGraderingArbeidsforholdDtoTjeneste {

    private FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste;
    private BeregningsgrunnlagDtoUtil dtoUtil;

    public RefusjonEllerGraderingArbeidsforholdDtoTjeneste() {
        // For CDI
    }

    @Inject
    public RefusjonEllerGraderingArbeidsforholdDtoTjeneste(FordelBeregningsgrunnlagTjeneste refusjonOgGraderingTjeneste,
                                                           BeregningsgrunnlagDtoUtil dtoUtil) {
        this.refusjonOgGraderingTjeneste = refusjonOgGraderingTjeneste;
        this.dtoUtil = dtoUtil;
    }

    List<FordelBeregningsgrunnlagArbeidsforholdDto> lagListeMedDtoForArbeidsforholdSomSøkerRefusjonEllerGradering(BeregningsgrunnlagInput input) {
        List<BeregningsgrunnlagPeriode> perioder = input.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder();
        var aktivitetGradering = input.getAktivitetGradering();
        var beregningAktivitetAggregat = input.getBeregningsgrunnlagGrunnlag().getGjeldendeAktiviteter();
        List<BeregningsgrunnlagPrStatusOgAndel> beregningsgrunnlagPrStatusOgAndel = perioder.stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> refusjonOgGraderingTjeneste.vurderManuellBehandlingForAndel(andel.getBeregningsgrunnlagPeriode(), andel, aktivitetGradering, beregningAktivitetAggregat, input.getInntektsmeldinger()).isPresent())
            .filter(andel -> Boolean.FALSE.equals(andel.getLagtTilAvSaksbehandler()))
            .distinct()
            .collect(Collectors.toList());

        LocalDate stp = input.getBeregningsgrunnlag().getSkjæringstidspunkt();

        return beregningsgrunnlagPrStatusOgAndel.stream()
            .map(distinctAndel -> mapTilEndretArbeidsforholdDto(input, distinctAndel, stp))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(af -> !af.getPerioderMedGraderingEllerRefusjon().isEmpty())
            .collect(Collectors.toList());
    }

    private Optional<FordelBeregningsgrunnlagArbeidsforholdDto> mapTilEndretArbeidsforholdDto(BeregningsgrunnlagInput input,
                                                                                              BeregningsgrunnlagPrStatusOgAndel distinctAndel,
                                                                                              LocalDate stp) {
        return dtoUtil.lagArbeidsforholdEndringDto(distinctAndel, input.getIayGrunnlag())
            .map(af -> {
                FordelBeregningsgrunnlagArbeidsforholdDto endringAf = (FordelBeregningsgrunnlagArbeidsforholdDto) af;
                settEndretArbeidsforholdForNyttRefusjonskrav(distinctAndel, endringAf);
                settEndretArbeidsforholdForSøktGradering(distinctAndel, endringAf, input.getAktivitetGradering());
                distinctAndel.getBgAndelArbeidsforhold().flatMap(bga ->
                    UtledBekreftetPermisjonerTilDto.utled(input.getIayGrunnlag(), stp, bga)
                ).ifPresent(endringAf::setPermisjon);

                return endringAf;
            });
    }

    private void settEndretArbeidsforholdForNyttRefusjonskrav(BeregningsgrunnlagPrStatusOgAndel distinctAndel,
                                                              FordelBeregningsgrunnlagArbeidsforholdDto endretArbeidsforhold) {
        List<Periode> refusjonsperioder = finnRefusjonsperioderForAndel(distinctAndel);
        refusjonsperioder.forEach(refusjonsperiode -> {
            GraderingEllerRefusjonDto refusjonDto = new GraderingEllerRefusjonDto(true, false);
            refusjonDto.setFom(refusjonsperiode.getFomOrNull());
            refusjonDto.setTom(TIDENES_ENDE.minusDays(2).isBefore(refusjonsperiode.getTom()) ? null : refusjonsperiode.getTom());
            endretArbeidsforhold.leggTilPeriodeMedGraderingEllerRefusjon(refusjonDto);
        });
    }

    private List<Periode> finnRefusjonsperioderForAndel(BeregningsgrunnlagPrStatusOgAndel distinctAndel) {
        List<Periode> refusjonsperioder = new ArrayList<>();
        BeregningsgrunnlagEntitet bg = distinctAndel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag();
        List<BeregningsgrunnlagPeriode> perioder = bg.getBeregningsgrunnlagPerioder();
        LocalDate sluttDatoRefusjon = TIDENES_BEGYNNELSE;
        for (int i = 0; i < perioder.size(); i++) {
            BeregningsgrunnlagPeriode periode = perioder.get(i);
            LocalDate tomDatoPeriode = periode.getBeregningsgrunnlagPeriodeTom() == null ?
                TIDENES_ENDE : periode.getBeregningsgrunnlagPeriodeTom();
            if (sluttDatoRefusjon.isBefore(tomDatoPeriode)) {
                Optional<BigDecimal> refusjonBeløpOpt = finnRefusjonsbeløpForAndelIPeriode(distinctAndel, periode);
                if (refusjonBeløpOpt.isPresent()) {
                    LocalDate startdatoRefusjon = periode.getBeregningsgrunnlagPeriodeFom();
                    sluttDatoRefusjon = finnSluttdato(distinctAndel, perioder, i, refusjonBeløpOpt.get());
                    refusjonsperioder.add(new Periode(startdatoRefusjon, sluttDatoRefusjon));
                }
            }
        }
        return refusjonsperioder;
    }

    private LocalDate finnSluttdato(BeregningsgrunnlagPrStatusOgAndel distinctAndel, List<BeregningsgrunnlagPeriode> perioder, int i, BigDecimal refusjonBeløp) {
        LocalDate sluttDatoRefusjon = TIDENES_ENDE;
        if (i == perioder.size() - 1) {
            return sluttDatoRefusjon;
        }
        for (int k = i + 1; k < perioder.size(); k++) {
            BeregningsgrunnlagPeriode nestePeriode = perioder.get(k);
            BigDecimal refusjonINestePeriode = finnRefusjonsbeløpForAndelIPeriode(distinctAndel, nestePeriode).orElse(BigDecimal.ZERO);
            if (refusjonINestePeriode.compareTo(refusjonBeløp) != 0) {
                return perioder.get(k - 1).getBeregningsgrunnlagPeriodeTom();
            }
        }
        return sluttDatoRefusjon;
    }

    private Optional<BigDecimal> finnRefusjonsbeløpForAndelIPeriode(BeregningsgrunnlagPrStatusOgAndel distinctAndel,
                                                                    BeregningsgrunnlagPeriode periode) {
        Optional<BeregningsgrunnlagPrStatusOgAndel> matchendeAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(andel -> !andel.getLagtTilAvSaksbehandler())
            .filter(andel -> andel.gjelderSammeArbeidsforhold(distinctAndel))
            .filter(andel -> andel.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) != 0)
            .findFirst();
        return matchendeAndel
            .flatMap(andel -> andel.getBgAndelArbeidsforhold()
                .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            );
    }

    private void settEndretArbeidsforholdForSøktGradering(BeregningsgrunnlagPrStatusOgAndel distinctAndel,
                                                          FordelBeregningsgrunnlagArbeidsforholdDto endretArbeidsforhold,
                                                          AktivitetGradering aktivitetGradering) {
        List<Gradering> graderingerForArbeidsforhold = FordelBeregningsgrunnlagTjeneste.hentGraderingerForAndel(distinctAndel, aktivitetGradering);
        graderingerForArbeidsforhold.forEach(gradering -> {
            GraderingEllerRefusjonDto graderingDto = new GraderingEllerRefusjonDto(false, true);
            DatoIntervallEntitet periode = gradering.getPeriode();
            graderingDto.setFom(periode.getFomDato());
            graderingDto.setTom(periode.getTomDato().isBefore(TIDENES_ENDE) ? periode.getTomDato() : null);
            endretArbeidsforhold.leggTilPeriodeMedGraderingEllerRefusjon(graderingDto);
        });
    }

}
