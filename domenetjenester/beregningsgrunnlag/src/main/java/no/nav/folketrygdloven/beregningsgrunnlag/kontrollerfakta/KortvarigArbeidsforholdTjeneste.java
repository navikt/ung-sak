package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.typer.AktørId;

public class KortvarigArbeidsforholdTjeneste {

    private KortvarigArbeidsforholdTjeneste() {
        // Skjul
    }

    private static boolean brukerHarStatusSN(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder()
            .stream()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList)
            .flatMap(Collection::stream)
            .anyMatch(bpsa -> bpsa.getAktivitetStatus().erSelvstendigNæringsdrivende());
    }

    private static Optional<Yrkesaktivitet> finnKorresponderendeYrkesaktivitet(Collection<Yrkesaktivitet> kortvarigeArbeidsforhold,
                                                                               Optional<Arbeidsgiver> arbeidsgiverOpt,
                                                                               Optional<InternArbeidsforholdRef> arbeidsforholdRefOpt) {

        return arbeidsgiverOpt.flatMap(arbeidsgiver -> kortvarigeArbeidsforhold.stream()
            .filter(ya -> ya.gjelderFor(arbeidsgiver, arbeidsforholdRefOpt.orElse(InternArbeidsforholdRef.nullRef())))
            .findFirst());
    }

    private static Collection<Yrkesaktivitet> hentKortvarigeYrkesaktiviteter(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                             InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        var filter = hentYrkesaktiviteter(aktørId, inntektArbeidYtelseGrunnlag, beregningsgrunnlag.getSkjæringstidspunkt());
        Collection<Yrkesaktivitet> yrkesAktiviteterOrdArb = filter.getYrkesaktiviteterForBeregning().stream()
            .filter(ya -> ya.getArbeidType().equals(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)).collect(Collectors.toList());

        return yrkesAktiviteterOrdArb.stream()
            .filter(ya -> erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(filter, beregningsgrunnlag, ya))
            .collect(Collectors.toList());
    }

    private static YrkesaktivitetFilter hentYrkesaktiviteter(AktørId aktørId, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                             LocalDate skjæringstidspunkt) {
        var aktørArbeid = inntektArbeidYtelseGrunnlag.getAktørArbeidFraRegister(aktørId);
        var filter = new YrkesaktivitetFilter(inntektArbeidYtelseGrunnlag.getArbeidsforholdInformasjon(), aktørArbeid);
        return filter.før(skjæringstidspunkt);
    }

    static boolean erKortvarigYrkesaktivitetSomAvsluttesEtterSkjæringstidspunkt(YrkesaktivitetFilter filter, BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                Yrkesaktivitet yrkesaktivitet) {
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        List<LocalDateSegment<Boolean>> periodeSegmenter = ansettelsesPerioder
            .stream()
            .map(AktivitetsAvtale::getPeriode)
            .map(a -> new LocalDateSegment<>(a.getFomDato(), a.getTomDato(), true))
            .collect(Collectors.toList());

        LocalDateTimeline<Boolean> ansettelsesTidslinje = new LocalDateTimeline<>(periodeSegmenter, håndterOverlapp()).compress();

        return ansettelsesTidslinje.getDatoIntervaller()
            .stream()
            .filter(avtale -> starterFørOgSlutterEtter(beregningsgrunnlag.getSkjæringstidspunkt(), avtale))
            .anyMatch(KortvarigArbeidsforholdTjeneste::isDurationLessThan6Months);
    }

    private static LocalDateSegmentCombinator<Boolean, Boolean, Boolean> håndterOverlapp() {
        return (interlal, segment1, segment2) -> new LocalDateSegment<>(interlal, true);
    }

    private static boolean starterFørOgSlutterEtter(LocalDate skjæringstidspunkt, LocalDateInterval avtale) {
        return avtale.getFomDato().isBefore(skjæringstidspunkt) && !avtale.getTomDato().isBefore(skjæringstidspunkt);
    }

    private static boolean isDurationLessThan6Months(LocalDateInterval aa) {
        Period duration = aa.getFomDato().until(aa.getTomDato().plusDays(1));
        return duration.getYears() < 1 && duration.getMonths() < 6;
    }

    public static boolean harKortvarigeArbeidsforholdOgErIkkeSN(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        if (brukerHarStatusSN(beregningsgrunnlag)) {
            return false;
        }
        return !hentAndelerForKortvarigeArbeidsforhold(aktørId, beregningsgrunnlag, inntektArbeidYtelseGrunnlag).isEmpty();
    }

    public static Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> hentAndelerForKortvarigeArbeidsforhold(AktørId aktørId,
                                                                                                                BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                                                InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {

        List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder();
        if (!beregningsgrunnlagPerioder.isEmpty()) {
            // beregningsgrunnlagPerioder er sortert, tar utgangspunkt i første
            BeregningsgrunnlagPeriode beregningsgrunnlagPeriode = beregningsgrunnlagPerioder.get(0);
            Collection<Yrkesaktivitet> kortvarigeArbeidsforhold = hentKortvarigeYrkesaktiviteter(aktørId, beregningsgrunnlag,
                inntektArbeidYtelseGrunnlag);

            return beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(prStatus -> prStatus.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSTAKER))
                .filter(andel -> finnKorresponderendeYrkesaktivitet(
                    kortvarigeArbeidsforhold,
                    andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver),
                    andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef)).isPresent())
                .collect(Collectors.toMap(Function.identity(),
                    andel -> finnKorresponderendeYrkesaktivitet(
                        kortvarigeArbeidsforhold,
                        andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsgiver),
                        andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef)).get()));
        } else {
            throw new IllegalArgumentException("Beregningsgrunnlag må ha minst ein periode");
        }
    }
}
