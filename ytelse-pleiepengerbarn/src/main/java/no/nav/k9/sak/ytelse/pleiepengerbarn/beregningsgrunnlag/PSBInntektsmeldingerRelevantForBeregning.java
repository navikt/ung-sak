package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.kompletthet.KompletthetForBeregningTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
public class PSBInntektsmeldingerRelevantForBeregning implements InntektsmeldingerRelevantForBeregning {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;

    public PSBInntektsmeldingerRelevantForBeregning() {
    }

    @Inject
    public PSBInntektsmeldingerRelevantForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
    }

    @Override
    public Collection<Inntektsmelding> begrensSakInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        var relevantPeriode = kompletthetForBeregningTjeneste.utledRelevantPeriode(referanse, vilkårsPeriode);
        return kompletthetForBeregningTjeneste.utledRelevanteInntektsmeldinger(new HashSet<>(sakInntektsmeldinger), relevantPeriode);
    }

    @Override
    public List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        var cutoffDato = utledCutOffDato(relevantPeriode);
        var sortedIms = sakInntektsmeldinger
            .stream()
            .filter(it -> datoErFørEllerLik(cutoffDato, it))
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return sorterOgPlukkUtPrioritert(relevantPeriode, sortedIms);
    }

    private boolean datoErFørEllerLik(LocalDate cutoffDato, Inntektsmelding it) {
        return it.getStartDatoPermisjon().isPresent() &&
            (it.getStartDatoPermisjon().get().isBefore(cutoffDato) || it.getStartDatoPermisjon().get().equals(cutoffDato));
    }

    private LocalDate utledCutOffDato(DatoIntervallEntitet relevantPeriode) {
        var maksdatoPeriode = relevantPeriode.getTomDato();
        var utledetCutOff = relevantPeriode.getFomDato().plusDays(29);

        if (maksdatoPeriode.isBefore(utledetCutOff)) {
            return utledetCutOff;
        }
        return maksdatoPeriode;
    }

    ArrayList<Inntektsmelding> sorterOgPlukkUtPrioritert(DatoIntervallEntitet relevantPeriode, Set<Inntektsmelding> sortedIms) {
        var inntektsmeldingene = new ArrayList<Inntektsmelding>();

        for (Inntektsmelding inntektsmelding : sortedIms) {
            if (harIngenInntektsmeldingerForArbeidsforholdIdentifikatoren(inntektsmeldingene, inntektsmelding)
                && inntektsmelding.getStartDatoPermisjon().isPresent()) {
                inntektsmeldingene.add(inntektsmelding);
            } else if (harInntektsmeldingSomMatcherArbeidsforhold(inntektsmeldingene, inntektsmelding)
                && skalErstatteEksisterendeInntektsmelding(inntektsmelding, inntektsmeldingene, relevantPeriode)) {
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
