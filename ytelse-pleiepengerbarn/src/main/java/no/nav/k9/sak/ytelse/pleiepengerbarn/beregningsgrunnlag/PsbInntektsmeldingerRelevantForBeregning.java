package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PsbInntektsmeldingerRelevantForBeregning implements InntektsmeldingerRelevantForBeregning {

    @Override
    public List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        var inntektsmeldingene = new ArrayList<Inntektsmelding>();

        var sortedIms = sakInntektsmeldinger.stream()
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        for (Inntektsmelding inntektsmelding : sortedIms) {
            if (harIngenInntektsmeldingerForArbeidsforholdIdentifikatoren(inntektsmeldingene, inntektsmelding)) {
                inntektsmeldingene.add(inntektsmelding);
            } else if (harInntektsmeldingSomMatcherArbeidsforhold(inntektsmeldingene, inntektsmelding)
                && skalErstatteEksisterendeInntektsmelding(inntektsmelding, inntektsmeldingene, vilkårsPeriode)) {
                inntektsmeldingene.removeIf(arbeidsforholdMatcher(inntektsmelding));
                inntektsmeldingene.add(inntektsmelding);
            }
        }

        return inntektsmeldingene;
    }

    private boolean skalErstatteEksisterendeInntektsmelding(Inntektsmelding inntektsmelding, List<Inntektsmelding> inntektsmeldingene, DatoIntervallEntitet vilkårsPeriode) {
        var datoNærmestSkjæringstidspunktet = inntektsmelding.getStartDatoPermisjon();
        if (datoNærmestSkjæringstidspunktet.isEmpty()) {
            return false;
        }
        var inntektsmeldingerSomErNærmereEllerNyere = inntektsmeldingene.stream()
            .filter(arbeidsforholdMatcher(inntektsmelding))
            .filter(it -> erNærmereEllerLikeNæreSkjæringtidspunktet(it, datoNærmestSkjæringstidspunktet.get(), vilkårsPeriode.getFomDato())
                && inntektsmelding.erNyereEnn(it))
            .collect(Collectors.toList());

        return !inntektsmeldingerSomErNærmereEllerNyere.isEmpty();
    }

    private boolean erNærmereEllerLikeNæreSkjæringtidspunktet(Inntektsmelding gammel, LocalDate nyInntektsmeldingDatoNærmestStp, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = gammel.getStartDatoPermisjon().orElseThrow();
        long distGammel = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
        long distNy = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, nyInntektsmeldingDatoNærmestStp));
        return distNy <= distGammel;
    }

    private boolean harInntektsmeldingSomMatcherArbeidsforhold(List<Inntektsmelding> inntektsmeldingene, Inntektsmelding inntektsmelding) {
        return inntektsmeldingene.stream().anyMatch(arbeidsforholdMatcher(inntektsmelding));
    }

    private boolean harIngenInntektsmeldingerForArbeidsforholdIdentifikatoren(List<Inntektsmelding> inntektsmeldingene, Inntektsmelding inntektsmelding) {
        return inntektsmeldingene.stream().noneMatch(arbeidsforholdMatcher(inntektsmelding));
    }

    private Predicate<Inntektsmelding> arbeidsforholdMatcher(Inntektsmelding inntektsmelding) {
        return it -> it.gjelderSammeArbeidsforhold(inntektsmelding);
    }
}
