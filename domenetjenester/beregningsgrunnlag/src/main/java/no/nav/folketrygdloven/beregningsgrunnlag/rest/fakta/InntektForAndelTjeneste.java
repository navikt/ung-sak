package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.iay.ArbeidType;

class InntektForAndelTjeneste {

    private static final int MND_I_1_ÅR = 12;

    private InntektForAndelTjeneste() {
        // Hide pulbic constructor
    }

    static BigDecimal finnSnittinntektForArbeidstakerIBeregningsperioden(InntektFilter filter, BeregningsgrunnlagPrStatusOgAndel andel) {
        LocalDate fraDato = andel.getBeregningsperiodeFom();
        LocalDate tilDato = andel.getBeregningsperiodeTom();
        long beregningsperiodeLengdeIMnd = ChronoUnit.MONTHS.between(fraDato, tilDato.plusDays(1));
        BigDecimal totalBeløp = finnTotalbeløpIBeregningsperioden(filter, andel, tilDato, beregningsperiodeLengdeIMnd);
        return totalBeløp.divide(BigDecimal.valueOf(beregningsperiodeLengdeIMnd), 10, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal finnTotalbeløpIBeregningsperioden(InntektFilter filter, BeregningsgrunnlagPrStatusOgAndel andel, LocalDate tilDato,
                                                                Long beregningsperiodeLengdeIMnd) {
        if(filter.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var inntekter = finnInntekterForAndel(andel, filter);

        AtomicReference<BigDecimal> totalBeløp = new AtomicReference<>(BigDecimal.ZERO);
        inntekter.forFilter((inntekt, inntektsposter) -> totalBeløp
            .set(totalBeløp.get().add(summerInntekterIBeregningsperioden(tilDato, inntektsposter, beregningsperiodeLengdeIMnd))));

        return totalBeløp.get();
    }

    static BigDecimal finnSnittinntektPrÅrForArbeidstakerIBeregningsperioden(InntektFilter filter, BeregningsgrunnlagPrStatusOgAndel andel) {
        if(filter.isEmpty()) {
            return BigDecimal.ZERO;
        }
        LocalDate fraDato = andel.getBeregningsperiodeFom();
        LocalDate tilDato = andel.getBeregningsperiodeTom();
        Long beregningsperiodeLengdeIMnd = ChronoUnit.MONTHS.between(fraDato, tilDato.plusDays(1));
        BigDecimal totalBeløp = finnTotalbeløpIBeregningsperioden(filter, andel, tilDato, beregningsperiodeLengdeIMnd);
        BigDecimal faktor = BigDecimal.valueOf(MND_I_1_ÅR).divide(BigDecimal.valueOf(beregningsperiodeLengdeIMnd), 10, RoundingMode.HALF_EVEN);
        return totalBeløp.multiply(faktor);
    }

    private static InntektFilter finnInntekterForAndel(BeregningsgrunnlagPrStatusOgAndel andel, InntektFilter filter) {
        Optional<Arbeidsgiver> arbeidsgiver = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver);
        if (!arbeidsgiver.isPresent()) {
            return InntektFilter.EMPTY;
        }
        return filter.filterBeregningsgrunnlag()
            .filter(arbeidsgiver.get());
    }

    private static BigDecimal summerInntekterIBeregningsperioden(LocalDate tilDato, Collection<Inntektspost> inntektsposter, Long beregningsperiodeLengdeIMnd) {
        BigDecimal totalBeløp = BigDecimal.ZERO;
        for (int måned = 0; måned < beregningsperiodeLengdeIMnd; måned++) {
            LocalDate dato = tilDato.minusMonths(måned);
            Beløp beløp = finnMånedsinntekt(inntektsposter, dato);
            totalBeløp = totalBeløp.add(beløp.getVerdi());
        }
        return totalBeløp;
    }

    private static Beløp finnMånedsinntekt(Collection<Inntektspost> inntektsposter, LocalDate dato) {
        return inntektsposter.stream()
            .filter(inntektspost -> inntektspost.getPeriode().inkluderer(dato))
            .findFirst().map(Inntektspost::getBeløp).orElse(Beløp.ZERO);
    }

    static Optional<BigDecimal> finnSnittAvFrilansinntektIBeregningsperioden(AktørId aktørId, InntektArbeidYtelseGrunnlag grunnlag,
                                                                             BeregningsgrunnlagPrStatusOgAndel frilansAndel, LocalDate skjæringstidspunkt) {
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt);
        if (!filter.isEmpty()) {
            LocalDate fraDato = frilansAndel.getBeregningsperiodeFom();
            LocalDate tilDato = frilansAndel.getBeregningsperiodeTom();
            long beregningsperiodeLengdeIMnd = ChronoUnit.MONTHS.between(fraDato, tilDato.plusDays(1));
            List<Yrkesaktivitet> yrkesaktiviteter = finnYrkesaktiviteter(aktørId, grunnlag, skjæringstidspunkt);
            boolean erFrilanser = yrkesaktiviteter.stream().anyMatch(ya -> ArbeidType.FRILANSER.equals(ya.getArbeidType()));

            var frilansInntekter = filter.filterBeregningsgrunnlag().filter(inntekt -> {
                var arbeidTyper = getArbeidTyper(yrkesaktiviteter, inntekt.getArbeidsgiver());
                return erFrilansInntekt(arbeidTyper, erFrilanser);
            });

            if (frilansInntekter.isEmpty()) {
                return Optional.empty();
            }
            AtomicReference<BigDecimal> totalBeløp = new AtomicReference<>(BigDecimal.ZERO);
            frilansInntekter.forFilter((inntekt, inntektsposter) -> totalBeløp
                .set(totalBeløp.get().add(summerInntekterIBeregningsperioden(tilDato, inntektsposter, beregningsperiodeLengdeIMnd))));
            return Optional.of(totalBeløp.get().divide(BigDecimal.valueOf(beregningsperiodeLengdeIMnd), 10, RoundingMode.HALF_EVEN));

        }
        return Optional.empty();
    }

    private static List<Yrkesaktivitet> finnYrkesaktiviteter(AktørId aktørId, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                             LocalDate skjæringstidspunkt) {
        List<Yrkesaktivitet> yrkesaktiviteter = new ArrayList<>();

        var aktørArbeid = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(aktørId);

        var filterRegister = new YrkesaktivitetFilter(inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon(), aktørArbeid).før(skjæringstidspunkt);
        yrkesaktiviteter.addAll(filterRegister.getYrkesaktiviteterForBeregning());
        yrkesaktiviteter.addAll(filterRegister.getFrilansOppdrag());

        var bekreftetAnnenOpptjening = inntektArbeidYtelseGrunnlag.getBekreftetAnnenOpptjening(aktørId);
        var filterSaksbehandlet = new YrkesaktivitetFilter(inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon(), bekreftetAnnenOpptjening);
        yrkesaktiviteter.addAll(filterSaksbehandlet.getYrkesaktiviteterForBeregning());

        return yrkesaktiviteter;
    }

    private static Collection<ArbeidType> getArbeidTyper(Collection<Yrkesaktivitet> yrkesaktiviteter, Arbeidsgiver arbeidsgiver) {
        return yrkesaktiviteter
            .stream()
            .filter(it -> it.getArbeidsgiver() != null)
            .filter(it -> it.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()))
            .map(Yrkesaktivitet::getArbeidType)
            .distinct()
            .collect(Collectors.toList());
    }

    private static boolean erFrilansInntekt(Collection<ArbeidType> arbeidTyper, boolean erFrilanser) {
        return (arbeidTyper.isEmpty() && erFrilanser) || arbeidTyper.contains(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER);
    }

}
