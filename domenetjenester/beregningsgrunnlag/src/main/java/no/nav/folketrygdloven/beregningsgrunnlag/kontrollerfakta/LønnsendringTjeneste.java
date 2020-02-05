package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.typer.AktørId;

public class LønnsendringTjeneste {

    private LønnsendringTjeneste() {
        // Skjul
    }

    public static boolean brukerHarHattLønnsendringOgManglerInntektsmelding(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        List<Yrkesaktivitet> aktiviteterMedLønnsendringUtenIM = finnAlleAktiviteterMedLønnsendringUtenInntektsmelding(aktørId, beregningsgrunnlag, iayGrunnlag);
        return !aktiviteterMedLønnsendringUtenIM.isEmpty();
    }

    public static List<Yrkesaktivitet> finnAlleAktiviteterMedLønnsendringUtenInntektsmelding(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var manglendeInntektsmeldinger = iayGrunnlag.getInntektsmeldingerSomIkkeKommer();
        if (manglendeInntektsmeldinger.isEmpty()) {
            return Collections.emptyList();
        }
        LocalDate skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();

        Optional<AktørArbeid> aktørArbeid = iayGrunnlag.getAktørArbeidFraRegister(aktørId);

        List<BeregningsgrunnlagPrStatusOgAndel> arbeidstakerAndeler = alleArbeidstakerandeler(beregningsgrunnlag);

        if (!aktørArbeid.isPresent() || arbeidstakerAndeler.isEmpty()) {
            return Collections.emptyList();
        }
        // Alle arbeidstakerandeler har samme beregningsperiode, kan derfor ta fra den første
        LocalDate beregningsperiodeFom = arbeidstakerAndeler.get(0).getBeregningsperiodeFom();
        LocalDate beregningsperiodeTom = arbeidstakerAndeler.get(0).getBeregningsperiodeTom();
        if (beregningsperiodeFom == null || beregningsperiodeTom == null) {
            return Collections.emptyList();
        }

        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), aktørArbeid).før(skjæringstidspunkt);
        Collection<Yrkesaktivitet> aktiviteterMedLønnsendring = finnAktiviteterMedLønnsendringIBeregningsperioden(filter, beregningsperiodeFom, beregningsperiodeTom, skjæringstidspunkt);
        if (aktiviteterMedLønnsendring.isEmpty()) {
            return Collections.emptyList();
        }
        return aktiviteterMedLønnsendring.stream()
            .filter(ya -> ya.getArbeidsgiver() != null && ya.getArbeidsgiver().getIdentifikator() != null)
            .filter(ya -> finnesKorresponderendeBeregningsgrunnlagsandel(arbeidstakerAndeler, ya))
            .filter(ya -> matchYrkesaktivitetMedInntektsmeldingSomIkkeKommer(manglendeInntektsmeldinger, ya))
            .collect(Collectors.toList());
    }

    private static boolean finnesKorresponderendeBeregningsgrunnlagsandel(List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                                                   Yrkesaktivitet a) {
        return andeler.stream()
            .anyMatch(andel -> andel.gjelderSammeArbeidsforhold(a.getArbeidsgiver(), a.getArbeidsforholdRef()));
    }

    private static List<BeregningsgrunnlagPrStatusOgAndel> alleArbeidstakerandeler(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(BeregningsgrunnlagPeriode::getBeregningsgrunnlagPrStatusOgAndelList).flatMap(Collection::stream)
            .filter(bpsa -> bpsa.getAktivitetStatus().erArbeidstaker())
            .collect(Collectors.toList());
    }

    private static boolean matchYrkesaktivitetMedInntektsmeldingSomIkkeKommer(List<InntektsmeldingSomIkkeKommer> manglendeInntektsmeldinger, Yrkesaktivitet yrkesaktivitet) {
        return manglendeInntektsmeldinger.stream()
            .anyMatch(im -> yrkesaktivitet.gjelderFor(im.getArbeidsgiver(), im.getRef()));
    }

    private static Collection<Yrkesaktivitet> finnAktiviteterMedLønnsendringIBeregningsperioden(YrkesaktivitetFilter filter, LocalDate beregningsperiodeFom, LocalDate beregningsperiodeTom, LocalDate skjæringstidspunkt) {
        return filter.getYrkesaktiviteterForBeregning()
            .stream()
            .filter(ya -> !ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(ya.getArbeidType())
                && !ArbeidType.FRILANSER.equals(ya.getArbeidType()))
            .filter(ya -> filter.getAnsettelsesPerioder(ya).stream()
                .anyMatch(ap -> ap.getPeriode().inkluderer(skjæringstidspunkt)))
            .filter(ya -> harAvtalerMedLønnsendringIBeregningsgrunnlagperioden(filter.getAktivitetsAvtalerForArbeid(ya), beregningsperiodeFom, beregningsperiodeTom))
            .collect(Collectors.toList());
    }

    private static boolean harAvtalerMedLønnsendringIBeregningsgrunnlagperioden(Collection<AktivitetsAvtale> aktivitetsAvtaler, LocalDate beregningsperiodeFom, LocalDate beregningsperiodeTom) {
        return !aktivitetsAvtaler
            .stream()
            .filter(aa -> aa.getSisteLønnsendringsdato() != null)
            .filter(aa -> aa.getSisteLønnsendringsdato().equals(beregningsperiodeFom) || aa.getSisteLønnsendringsdato().isAfter(beregningsperiodeFom))
            .filter(aa -> aa.getSisteLønnsendringsdato().equals(beregningsperiodeTom) || aa.getSisteLønnsendringsdato().isBefore(beregningsperiodeTom))
            .collect(Collectors.toList())
            .isEmpty();
    }
}
