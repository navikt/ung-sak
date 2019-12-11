package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningInntektsmeldingTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FordelBeregningsgrunnlagAndelDto;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

/**
 * Tjeneste som henter ut informasjon relatert til refusjon fra beregnigsgrunnlag og setter det på dto-objekt
 */
class RefusjonDtoTjeneste {

    private RefusjonDtoTjeneste() {
        // Hide constructor
    }

    /**
     * Utleder om gitt andel har informasjon i inntektsmelding som krever at man skal kunne flytte refusjon i perioden
     *
     * @param andelFraOppdatert Beregnignsgrunnlagsandel fra oppdatert grunnlag
     * @param aktivitetGradering Graderinger for behandling
     * @param inntektsmeldinger inntektsmeldinger for behandling
     * @return Returnerer true om andel har gradering (uten refusjon) og total refusjon i perioden er større enn 6G, ellers false
     */
    static boolean skalKunneEndreRefusjon(BeregningsgrunnlagPrStatusOgAndel andelFraOppdatert,
                                          AktivitetGradering aktivitetGradering, Collection<Inntektsmelding> inntektsmeldinger) {
        if (harGraderingOgIkkeRefusjon(andelFraOppdatert, aktivitetGradering)) {
            return BeregningInntektsmeldingTjeneste.erTotaltRefusjonskravStørreEnnEllerLikSeksG(andelFraOppdatert.getBeregningsgrunnlagPeriode(), inntektsmeldinger);
        }
        return false;
    }

    private static boolean harGraderingOgIkkeRefusjon(BeregningsgrunnlagPrStatusOgAndel andelFraOppdatert, AktivitetGradering aktivitetGradering) {
        List<Gradering> graderingForAndelIPeriode = FordelBeregningsgrunnlagTjeneste.hentGraderingerForAndelIPeriode(andelFraOppdatert, aktivitetGradering);
        boolean andelHarGradering = !graderingForAndelIPeriode.isEmpty();
        BigDecimal refusjon = andelFraOppdatert.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(BigDecimal.ZERO);
        boolean andelHarRefusjon = refusjon.compareTo(BigDecimal.ZERO) > 0;
        return andelHarGradering && !andelHarRefusjon;
    }

    /**
     * Setter refusjonkrav på dto-objekt for gitt andel, både beløp fra inntektsmelding og fastsatt refusjon (redigert i fakta om beregning)
     *  @param andel Beregningsgrunnlagandel
     * @param periode Periode korresponderende til en beregningsgrunnlagperiode
     * @param endringAndel Dto for andel
     * @param inntektsmeldinger
     */
    static void settRefusjonskrav(BeregningsgrunnlagPrStatusOgAndel andel, ÅpenDatoIntervallEntitet periode, FordelBeregningsgrunnlagAndelDto endringAndel, Collection<Inntektsmelding> inntektsmeldinger) {
        if (andel.getLagtTilAvSaksbehandler()) {
            endringAndel.setRefusjonskravFraInntektsmeldingPrÅr(BigDecimal.ZERO);
        } else {
            Optional<BigDecimal> refusjonsKravPrÅr = BeregningInntektsmeldingTjeneste.finnRefusjonskravPrÅrIPeriodeForAndel(andel, periode, inntektsmeldinger);
            refusjonsKravPrÅr.ifPresent(endringAndel::setRefusjonskravFraInntektsmeldingPrÅr);
        }
        endringAndel.setRefusjonskravPrAar(andel.getBgAndelArbeidsforhold()
            .map(BGAndelArbeidsforhold::getRefusjonskravPrÅr)
            .orElse(BigDecimal.ZERO));
    }

    /**
     * Adderer refusjon for andeler i samme arbeidsforhold og setter det på andelen som ikke er lagt til av saksbehandler
     *
     * @param endringAndeler Liste med Dto-objekt for andeler
     */
    static void slåSammenRefusjonForAndelerISammeArbeidsforhold(List<FordelBeregningsgrunnlagAndelDto> endringAndeler) {
        Map<BeregningsgrunnlagArbeidsforholdDto, BigDecimal> totalRefusjonMap = getTotalrefusjonPrArbeidsforhold(endringAndeler);
        endringAndeler.forEach(andel -> {
            if (harArbeidsforholdOgErIkkjeLagtTilAvSaksbehandler(andel)) {
                BeregningsgrunnlagArbeidsforholdDto arbeidsforhold = andel.getArbeidsforhold();
                BigDecimal totalRefusjonForArbeidsforhold = totalRefusjonMap.get(arbeidsforhold);
                andel.setRefusjonskravPrAar(totalRefusjonForArbeidsforhold != null ? totalRefusjonForArbeidsforhold : andel.getRefusjonskravPrAar());
            } else if (harArbeidsforholdOgErLagtTilManuelt(andel)) {
                BeregningsgrunnlagArbeidsforholdDto arbeidsforhold = andel.getArbeidsforhold();
                BigDecimal totalRefusjonForArbeidsforhold = totalRefusjonMap.get(arbeidsforhold);
                andel.setRefusjonskravPrAar(totalRefusjonForArbeidsforhold != null ? null : andel.getRefusjonskravPrAar());
            }
        });
    }

    private static Map<BeregningsgrunnlagArbeidsforholdDto, BigDecimal> getTotalrefusjonPrArbeidsforhold(List<FordelBeregningsgrunnlagAndelDto> andeler) {
        Map<BeregningsgrunnlagArbeidsforholdDto, BigDecimal> arbeidsforholdRefusjonMap = new HashMap<>();
        andeler.forEach(andel -> {
            if (andel.getArbeidsforhold() != null) {
                BeregningsgrunnlagArbeidsforholdDto arbeidsforhold = andel.getArbeidsforhold();
                BigDecimal refusjonskrav = andel.getRefusjonskravPrAar() == null ?
                    BigDecimal.ZERO : andel.getRefusjonskravPrAar();
                if (arbeidsforholdRefusjonMap.containsKey(arbeidsforhold)) {
                    BigDecimal totalRefusjon = arbeidsforholdRefusjonMap.get(arbeidsforhold).add(refusjonskrav);
                    arbeidsforholdRefusjonMap.put(arbeidsforhold, totalRefusjon);
                } else {
                    arbeidsforholdRefusjonMap.put(arbeidsforhold, refusjonskrav);
                }
            }
        });
        return arbeidsforholdRefusjonMap;
    }

    private static boolean harArbeidsforholdOgErLagtTilManuelt(FordelBeregningsgrunnlagAndelDto andel) {
        return andel.getArbeidsforhold() != null && andel.getLagtTilAvSaksbehandler();
    }

    private static boolean harArbeidsforholdOgErIkkjeLagtTilAvSaksbehandler(FordelBeregningsgrunnlagAndelDto andel) {
        return andel.getArbeidsforhold() != null && !andel.getLagtTilAvSaksbehandler();
    }


}
