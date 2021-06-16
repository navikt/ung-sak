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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.pleiepengerbarn.kompletthetssjekk.KompletthetForBeregningTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PSBInntektsmeldingerRelevantForBeregning implements InntektsmeldingerRelevantForBeregning {

    private KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    public PSBInntektsmeldingerRelevantForBeregning() {
    }

    @Inject
    public PSBInntektsmeldingerRelevantForBeregning(KompletthetForBeregningTjeneste kompletthetForBeregningTjeneste,
                                                    VilkårResultatRepository vilkårResultatRepository) {
        this.kompletthetForBeregningTjeneste = kompletthetForBeregningTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @Override
    public Collection<Inntektsmelding> begrensSakInntektsmeldinger(BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        var tidslinje = utledTidslinje(referanse);
        var relevantPeriode = kompletthetForBeregningTjeneste.utledRelevantPeriode(tidslinje, vilkårsPeriode);
        return kompletthetForBeregningTjeneste.utledRelevanteInntektsmeldinger(new HashSet<>(sakInntektsmeldinger), relevantPeriode);
    }

    @Override
    public List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet relevantPeriode) {
        var sortedIms = sakInntektsmeldinger
            .stream()
            .sorted(Inntektsmelding.COMP_REKKEFØLGE)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return sorterOgPlukkUtPrioritert(relevantPeriode, sortedIms);
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

    private LocalDateTimeline<Boolean> utledTidslinje(BehandlingReferanse referanse) {
        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(referanse.getBehandlingId());
        if (vilkårene.isEmpty()) {
            return new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true)));
        }
        var vilkåret = vilkårene.get().getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);

        return vilkåret.map(vilkår -> new LocalDateTimeline<>(vilkår.getPerioder().stream()
            .map(VilkårPeriode::getPeriode)
            .map(DatoIntervallEntitet::toLocalDateInterval)
            .map(it -> new LocalDateSegment<>(it, true))
            .collect(Collectors.toList())))
            .orElseGet(() -> new LocalDateTimeline<>(List.of(new LocalDateSegment<>(referanse.getFagsakPeriode().toLocalDateInterval(), true))));
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
