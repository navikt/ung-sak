package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode.Søknadsperiode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttakPerioderGrunnlagRepository;

@ApplicationScoped
public class ArbeidBrukerBurdeSøktOmUtleder {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste;
    private UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    public ArbeidBrukerBurdeSøktOmUtleder() {
    }

    @Inject
    public ArbeidBrukerBurdeSøktOmUtleder(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          @FagsakYtelseTypeRef("PSB") @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste,
                                          @FagsakYtelseTypeRef("PSB") VurderSøknadsfristTjeneste<Søknadsperiode> søknadsfristTjeneste,
                                          UttakPerioderGrunnlagRepository uttakPerioderGrunnlagRepository,
                                          VilkårResultatRepository vilkårResultatRepository) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.uttakPerioderGrunnlagRepository = uttakPerioderGrunnlagRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    public Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledMangler(BehandlingReferanse referanse) {
        var vurderteSøknadsperioder = søknadsfristTjeneste.vurderSøknadsfrist(referanse);
        var vilkårene = vilkårResultatRepository.hent(referanse.getBehandlingId());
        var uttakGrunnlag = uttakPerioderGrunnlagRepository.hentGrunnlag(referanse.getBehandlingId()).orElseThrow();
        var perioderFraSøknader = uttakGrunnlag.getOppgitteSøknadsperioder()
            .getPerioderFraSøknadene();
        var perioderTilVurdering = finnSykdomsperioder(referanse);

        var tidslinjeTilVurdering = new LocalDateTimeline<>(perioderTilVurdering.stream().map(it -> new LocalDateSegment<>(it.toLocalDateInterval(), true)).collect(Collectors.toList()));

        var input = new ArbeidstidMappingInput()
            .medKravDokumenter(vurderteSøknadsperioder.keySet())
            .medPerioderFraSøknader(perioderFraSøknader)
            .medTidslinjeTilVurdering(tidslinjeTilVurdering)
            .medVilkår(vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET).orElseThrow());

        var aktørArbeidFraRegister = inntektArbeidYtelseTjeneste.hentGrunnlag(referanse.getBehandlingId()).getAktørArbeidFraRegister(referanse.getAktørId());
        return utledFraInput(tidslinjeTilVurdering, input, aktørArbeidFraRegister);
    }

    Map<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledFraInput(LocalDateTimeline<Boolean> tidslinjeTilVurdering, ArbeidstidMappingInput input, Optional<AktørArbeid> aktørArbeid) {
        var søktArbeid = new MapArbeid().mapTilRaw(input);

        if (tidslinjeTilVurdering.isEmpty()) {
            return Map.of();
        }

        return utledHvaSomBurdeVærtSøktOm(tidslinjeTilVurdering, aktørArbeid, søktArbeid);
    }

    private NavigableSet<DatoIntervallEntitet> finnSykdomsperioder(BehandlingReferanse referanse) {
        final var s1 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        final var s2 = perioderTilVurderingTjeneste.utled(referanse.getBehandlingId(), VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        final var resultat = new TreeSet<>(s1);
        resultat.addAll(s2);
        return resultat;
    }

    private HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> utledHvaSomBurdeVærtSøktOm(LocalDateTimeline<Boolean> tidslinjeTilVurdering, Optional<AktørArbeid> aktørArbeid, Map<AktivitetIdentifikator, LocalDateTimeline<WrappedArbeid>> søktArbeid) {
        var mellomregning = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();

        aktørArbeid.map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .filter(it -> ArbeidType.AA_REGISTER_TYPER.contains(it.getArbeidType()))
            .forEach(yrkesaktivitet -> mapYrkesAktivitet(mellomregning, yrkesaktivitet));

        var helgeTidslinje = utledTidslinjeMedMelgeneTilVurdering(tidslinjeTilVurdering);

        var resultat = new HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>>();
        // Sjekk mot hva det skulle vært søkt om
        for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<Boolean>> entry : mellomregning.entrySet()) {
            var søktOm = søktArbeid.getOrDefault(entry.getKey(), new LocalDateTimeline<WrappedArbeid>(List.of()));

            var skulleVærtSøktOm = entry.getValue().disjoint(søktOm);
            skulleVærtSøktOm = skulleVærtSøktOm.intersection(tidslinjeTilVurdering);
            skulleVærtSøktOm = skulleVærtSøktOm.disjoint(helgeTidslinje);

            if (!skulleVærtSøktOm.isEmpty()) {
                resultat.put(entry.getKey(), skulleVærtSøktOm);
            }
        }

        return resultat;
    }

    private LocalDateTimeline<Boolean> utledTidslinjeMedMelgeneTilVurdering(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var timeline = new LocalDateTimeline<Boolean>(List.of());
        for (LocalDateSegment<Boolean> segment : tidslinjeTilVurdering.toSegments()) {
            var min = segment.getFom();
            var max = segment.getTom();
            LocalDate next = min;

            while (next.isBefore(max)) {
                if (Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(next.getDayOfWeek()) && min.isEqual(next)) {
                    next = finnNeste(max, next);
                    timeline = timeline.combine(new LocalDateSegment<>(min, next, true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
                var start = finnNærmeste(DayOfWeek.SATURDAY, next);
                next = finnNeste(max, start);
                if (start.isBefore(max)) {
                    timeline = timeline.combine(new LocalDateSegment<>(start, next, true), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            }

        }
        return timeline;
    }

    private LocalDate finnNeste(LocalDate max, LocalDate start) {
        LocalDate next;
        next = finnNærmeste(DayOfWeek.SUNDAY, start);
        if (next.isAfter(max)) {
            next = max;
        }
        return next;
    }

    private LocalDate finnNærmeste(DayOfWeek target, LocalDate date) {
        var dayOfWeek = date.getDayOfWeek();
        if (target.equals(dayOfWeek)) {
            return date;
        }
        return finnNærmeste(target, date.plusDays(1));
    }

    private AktivitetIdentifikator utledIdentifikator(Yrkesaktivitet yrkesaktivitet) {
        return new AktivitetIdentifikator(UttakArbeidType.ARBEIDSTAKER, yrkesaktivitet.getArbeidsgiver(), null);
    }

    private void mapYrkesAktivitet(HashMap<AktivitetIdentifikator, LocalDateTimeline<Boolean>> resultat, Yrkesaktivitet yrkesaktivitet) {
        var key = utledIdentifikator(yrkesaktivitet);
        var arbeidsAktivTidslinje = resultat.getOrDefault(key, new LocalDateTimeline<>(List.of()));

        var segmenter = yrkesaktivitet.getAnsettelsesPeriode().stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
            .collect(Collectors.toList());
        // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
        for (LocalDateSegment<Boolean> segment : segmenter) {
            var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
            arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        }

        resultat.put(key, arbeidsAktivTidslinje.compress());
    }
}
