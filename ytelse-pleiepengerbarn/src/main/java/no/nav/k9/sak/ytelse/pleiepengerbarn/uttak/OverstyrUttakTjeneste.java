package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrUttakRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakPeriode;
import no.nav.k9.sak.behandlingslager.behandling.uttak.OverstyrtUttakUtbetalingsgrad;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;

@ApplicationScoped
public class OverstyrUttakTjeneste {

    private UttakTjeneste uttakTjeneste;
    private OverstyrUttakRepository overstyrUttakRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    public OverstyrUttakTjeneste() {
    }

    @Inject
    public OverstyrUttakTjeneste(UttakTjeneste uttakTjeneste,
                                 OverstyrUttakRepository overstyrUttakRepository,
                                 @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjeneste,
                                 VilkårResultatRepository vilkårResultatRepository) {
        this.overstyrUttakRepository = overstyrUttakRepository;
        this.uttakTjeneste = uttakTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    public boolean skalOverstyreUttak(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var vurderer = new SkalOverstyreUttakVurderer(overstyrUttakRepository, periodeTjeneste);
        return vurderer.skalOverstyreUttak(behandlingReferanse);
    }


    public void ryddMotVilkår(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var definerendeVilkår = periodeTjeneste.definerendeVilkår();
        var vilkårResultat = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId());
        var definerendeVilkårsperioder = definerendeVilkår.stream().map(vilkårResultat::getVilkår)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(v -> v.getPerioder().stream())
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        overstyrUttakRepository.ryddMotVilkår(behandlingReferanse.getBehandlingId(), definerendeVilkårsperioder);
    }


    public void ryddMotUttaksplan(BehandlingReferanse behandlingReferanse) {
        var uttaksplan = uttakTjeneste.hentUttaksplan(behandlingReferanse.getBehandlingUuid(), true);
        var overstyrtUttakTilVurdering = finnOverstyrtUttakTilVurdering(behandlingReferanse);
        var uttaksplanTidslinje = new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> new LocalDateSegment<>(e.getKey().getFom(), e.getKey().getTom(), e.getValue())).toList());
        var sletteListe = new ArrayList<Long>();
        var tidslinjeRyddetMotUttaksplan = overstyrtUttakTilVurdering.combine(uttaksplanTidslinje, ryddSegmenterMotUttaksplan(sletteListe), LocalDateTimeline.JoinStyle.INNER_JOIN);
        overstyrUttakRepository.oppdaterOverstyringAvUttak(behandlingReferanse.getBehandlingId(), sletteListe, tidslinjeRyddetMotUttaksplan.filterValue(Objects::nonNull));

    }

    private LocalDateTimeline<OverstyrtUttakPeriode> finnOverstyrtUttakTilVurdering(BehandlingReferanse behandlingReferanse) {
        var periodeTjeneste = VilkårsPerioderTilVurderingTjeneste.finnTjeneste(vilkårsPerioderTilVurderingTjeneste, behandlingReferanse.getFagsakYtelseType(), behandlingReferanse.getBehandlingType());
        var perioderTilVurdering = new LocalDateTimeline<>(periodeTjeneste.utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId()).stream().map(p -> new LocalDateSegment<>(p.toLocalDateInterval(), Boolean.TRUE)).collect(Collectors.toSet()));
        var overstyrtUttak = overstyrUttakRepository.hentOverstyrtUttak(behandlingReferanse.getBehandlingId());
        return overstyrtUttak.intersection(perioderTilVurdering);
    }

    private LocalDateSegmentCombinator<OverstyrtUttakPeriode, UttaksperiodeInfo, OverstyrtUttakPeriode> ryddSegmenterMotUttaksplan(List<Long> sletteListe) {
        return (di, lhs, rhs) -> {
            var utbetalingsgrader = rhs.getValue().getUtbetalingsgrader();
            var overstyrteUtbetalingsgrader = lhs.getValue().getOverstyrtUtbetalingsgrad();
            var overstyrteUtbetalingsgraderMedMatch = overstyrteUtbetalingsgrader.stream().filter(ou -> utbetalingsgrader.stream().anyMatch(u ->
                matcherAktivitet(ou, u))).collect(Collectors.toSet());


            if (overstyrteUtbetalingsgraderMedMatch.isEmpty()) {
                // behold id for sletting
                sletteListe.add(lhs.getValue().getId());
                return LocalDateSegment.emptySegment(di.getFomDato(), di.getTomDato());
            }

            return new LocalDateSegment<>(di, new OverstyrtUttakPeriode(null, lhs.getValue().getSøkersUttaksgrad(), overstyrteUtbetalingsgraderMedMatch, lhs.getValue().getBegrunnelse()));
        };
    }

    private boolean matcherAktivitet(OverstyrtUttakUtbetalingsgrad ou, Utbetalingsgrader u) {
        if (ou.getArbeidsgiver() == null) {
            return matcherAktivitetType(ou, u);
        }
        return matcherAktivitetType(ou, u) &&
            matcherArbeidsforhold(u.getArbeidsforhold(), ou);
    }

    private static boolean matcherAktivitetType(OverstyrtUttakUtbetalingsgrad ou, Utbetalingsgrader u) {
        return UttakArbeidType.fraKode(u.getArbeidsforhold().getType()).equals(ou.getAktivitetType());
    }


    private boolean matcherArbeidsforhold(Arbeidsforhold arbeidsforhold, OverstyrtUttakUtbetalingsgrad overstyrtUttakUtbetalingsgrad) {
        var arbeidsgiver = overstyrtUttakUtbetalingsgrad.getArbeidsgiver();
        var arbeidsforholdRef = overstyrtUttakUtbetalingsgrad.getInternArbeidsforholdRef();
        if (arbeidsgiver.getArbeidsgiverAktørId() != null) {
            return arbeidsforhold.getAktørId() != null
                && arbeidsgiver.getArbeidsgiverAktørId().equals(arbeidsforhold.getAktørId())
                && InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()).gjelderFor(arbeidsforholdRef);
        } else if (arbeidsgiver.getArbeidsgiverOrgnr() != null) {
            return Objects.equals(arbeidsgiver.getArbeidsgiverOrgnr(), arbeidsforhold.getOrganisasjonsnummer())
                && InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()).gjelderFor(arbeidsforholdRef);
        }
        return false;
    }


}
