package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class OmsorgspengerInntektsmeldingerRelevantForBeregning implements InntektsmeldingerRelevantForBeregning {

    private boolean lansertSjekkFørsteFraværsdag;

    private Period GRENSE_AKSEPTER_STARTDATO_FØR_VILKÅRSPERIODE = Period.ofWeeks(4);
    private Period GRENSE_AKSEPTER_STARTDATO_ETTER_VILKÅRSPERIODE = Period.ofDays(0);

    OmsorgspengerInntektsmeldingerRelevantForBeregning() {
    }

    @Inject
    public OmsorgspengerInntektsmeldingerRelevantForBeregning(@KonfigVerdi(value = "OMP_IM_SJEKK_FORSTE_FRAVAERSDAG", defaultVerdi = "true") boolean lansertSjekkFørsteFraværsdag) {
        this.lansertSjekkFørsteFraværsdag = lansertSjekkFørsteFraværsdag;
    }

    @Override
    public List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        var inntektsmeldingene = new ArrayList<Inntektsmelding>();

        var alleInntektsmeldinger = hentInntektsmeldingerSomGjelderForVilkårsperiode(sakInntektsmeldinger, vilkårsPeriode);
        for (Inntektsmelding inntektsmelding : alleInntektsmeldinger) {
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
        var datoNærmestSkjæringstidspunktet = finnDatoNærmestSkjæringstidspunktet(inntektsmelding, vilkårsPeriode.getFomDato());
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

    private Optional<LocalDate> finnDatoNærmestSkjæringstidspunktet(Inntektsmelding inntektsmelding, LocalDate skjæringstidspunkt) {
        if (lansertSjekkFørsteFraværsdag && inntektsmelding.getOppgittFravær().isEmpty()) {
            return inntektsmelding.getStartDatoPermisjon();
        }
        var inkludert = inntektsmelding.getOppgittFravær()
            .stream()
            .filter(at -> !Duration.ZERO.equals(at.getVarighetPerDag()))
            .filter(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFom(), p.getTom()).inkluderer(skjæringstidspunkt))
            .findFirst();
        if (inkludert.isPresent()) {
            return Optional.of(skjæringstidspunkt);
        }
        return inntektsmelding.getOppgittFravær()
            .stream()
            .filter(at -> !Duration.ZERO.equals(at.getVarighetPerDag()))
            .map(PeriodeAndel::getFom)
            .min(Comparator.comparingLong(x -> Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, x))));
    }

    private boolean erNærmereEllerLikeNæreSkjæringtidspunktet(Inntektsmelding gammel, LocalDate nyInntektsmeldingDatoNærmestStp, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(gammel, skjæringstidspunkt).orElseThrow();
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

    private Set<Inntektsmelding> hentInntektsmeldingerSomGjelderForVilkårsperiode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        LocalDateInterval intervallForAksepterbarStartdato = new LocalDateInterval(
            vilkårsPeriode.getFomDato().minus(GRENSE_AKSEPTER_STARTDATO_FØR_VILKÅRSPERIODE),
            vilkårsPeriode.getTomDato().plus(GRENSE_AKSEPTER_STARTDATO_ETTER_VILKÅRSPERIODE));

        Predicate<Inntektsmelding> filterGittFraværsperioder = it -> it.getOppgittFravær()
            .stream()
            .filter(at -> !Duration.ZERO.equals(at.getVarighetPerDag()))
            .anyMatch(at -> vilkårsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(at.getFom(), at.getTom())));

        Predicate<Inntektsmelding> filtrerGittFraværsperioderOgFørsteStønadsdag = im ->
            filterGittFraværsperioder.test(im) || im.getOppgittFravær().isEmpty() && im.getStartDatoPermisjon().isPresent() && intervallForAksepterbarStartdato.encloses(im.getStartDatoPermisjon().get());

        return sakInntektsmeldinger
            .stream()
            .filter(lansertSjekkFørsteFraværsdag ? filtrerGittFraværsperioderOgFørsteStønadsdag : filterGittFraværsperioder)
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Predicate<Inntektsmelding> arbeidsforholdMatcher(Inntektsmelding inntektsmelding) {
        return it -> it.gjelderSammeArbeidsforhold(inntektsmelding);
    }
}
