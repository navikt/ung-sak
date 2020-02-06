package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.OppgittFrilans;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;

public class KontrollerFaktaBeregningFrilanserTjeneste {

    private KontrollerFaktaBeregningFrilanserTjeneste() {
        // Skjul
    }

    public static boolean erNyoppstartetFrilanser(BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        boolean erFrilanser = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .anyMatch(andel -> andel.getAktivitetStatus().erFrilanser());

        return erFrilanser
            && iayGrunnlag.getOppgittOpptjening()
            .flatMap(OppgittOpptjening::getFrilans)
            .map(OppgittFrilans::getErNyoppstartet)
            .orElse(false);
    }

    public static boolean erBrukerArbeidstakerOgFrilanserISammeOrganisasjon(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return !brukerErArbeidstakerOgFrilanserISammeOrganisasjon(aktørId, beregningsgrunnlag, iayGrunnlag).isEmpty();
    }

    public static Set<Arbeidsgiver> brukerErArbeidstakerOgFrilanserISammeOrganisasjon(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return arbeidsgivereSomHarFrilansforholdOgArbeidsforholdMedBruker(iayGrunnlag, beregningsgrunnlag, aktørId);
    }

    private static Set<Arbeidsgiver> arbeidsgivereSomHarFrilansforholdOgArbeidsforholdMedBruker(InntektArbeidYtelseGrunnlag iayGrunnlag, BeregningsgrunnlagEntitet beregningsgrunnlag, AktørId aktørId) {

        // Sjekk om statusliste inneholder AT og FL.

        if (beregningsgrunnlag.getBeregningsgrunnlagPerioder().isEmpty() ||
            !harFrilanserOgArbeidstakerAndeler(beregningsgrunnlag)) {
            return Collections.emptySet();
        }

        // Sjekk om samme orgnr finnes både som arbeidsgiver og frilansoppdragsgiver

        final Set<Arbeidsgiver> arbeidsforholdArbeidsgivere = finnArbeidsgivere(beregningsgrunnlag);
        if (arbeidsforholdArbeidsgivere.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<Arbeidsgiver> frilansOppdragsgivere = finnFrilansOppdragsgivere(aktørId, beregningsgrunnlag, iayGrunnlag);
        if (frilansOppdragsgivere.isEmpty()) {
            return Collections.emptySet();
        }
        return finnMatchendeArbeidsgiver(arbeidsforholdArbeidsgivere, frilansOppdragsgivere);
    }

    private static boolean harFrilanserOgArbeidstakerAndeler(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
        .anyMatch(andel -> andel.getAktivitetStatus().erFrilanser()) &&
            beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream().flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .anyMatch(andel -> andel.getAktivitetStatus().erArbeidstaker());
    }

    private static Set<Arbeidsgiver> finnMatchendeArbeidsgiver(final Set<Arbeidsgiver> virksomheterForArbeidsforhold, final Set<Arbeidsgiver> frilansOppdragsgivere) {
        Set<Arbeidsgiver> intersection = new HashSet<>(virksomheterForArbeidsforhold);
        intersection.retainAll(frilansOppdragsgivere);
        return intersection;
    }

    private static Set<Arbeidsgiver> finnFrilansOppdragsgivere(AktørId aktørId, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        boolean erFrilanser = beregningsgrunnlag.getAktivitetStatuser().stream()
            .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
            .anyMatch(AktivitetStatus::erFrilanser);
        if (!erFrilanser) {
            return Collections.emptySet();
        }
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId)).før(beregningsgrunnlag.getSkjæringstidspunkt());

        return filter.getFrilansOppdrag()
            .stream()
            .map(Yrkesaktivitet::getArbeidsgiver)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private static Set<Arbeidsgiver> finnArbeidsgivere(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .filter(bpsa -> AktivitetStatus.ARBEIDSTAKER.equals(bpsa.getAktivitetStatus()))
            .map(BeregningsgrunnlagPrStatusOgAndel::getBgAndelArbeidsforhold)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(BGAndelArbeidsforhold::getArbeidsgiver)
            .collect(Collectors.toSet());
    }
}
