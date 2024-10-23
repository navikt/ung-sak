package no.nav.k9.sak.ytelse.ung.periode;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.UNGDOMSYTELSE;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.UtledeteVilkår;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.ung.inngangsvilkår.InngangsvilkårUtleder;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class UngdomsytelseVilkårsperioderTilVurderingTjeneste implements VilkårsPerioderTilVurderingTjeneste {

    private InngangsvilkårUtleder inngangsvilkårUtleder;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    UngdomsytelseVilkårsperioderTilVurderingTjeneste() {
        // CDI
    }

    @Inject
    public UngdomsytelseVilkårsperioderTilVurderingTjeneste(
        @FagsakYtelseTypeRef(UNGDOMSYTELSE) InngangsvilkårUtleder inngangsvilkårUtleder, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.inngangsvilkårUtleder = inngangsvilkårUtleder;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }


    @Override
    public KantIKantVurderer getKantIKantVurderer() {
        return new DefaultKantIKantVurderer();
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
        return utledPeriode(behandlingId);
    }

    @Override
    public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
        final var vilkårPeriodeSet = new HashMap<VilkårType, NavigableSet<DatoIntervallEntitet>>();
        UtledeteVilkår utledeteVilkår = inngangsvilkårUtleder.utledVilkår(null);
        utledeteVilkår.getAlleAvklarte()
            .forEach(vilkår -> vilkårPeriodeSet.put(vilkår, utledPeriode(behandlingId)));

        return vilkårPeriodeSet;
    }

    @Override
    public int maksMellomliggendePeriodeAvstand() {
        return 0;
    }

    @Override
    public Set<VilkårType> definerendeVilkår() {
        return Set.of(VilkårType.UNGDOMSPROGRAMVILKÅRET);
    }

    private TreeSet<DatoIntervallEntitet> utledPeriode(Long behandlingId) {
        var initieltGrunnlag = ungdomsprogramPeriodeRepository.hentInitieltGrunnlag(behandlingId);
        var ungdomsprogramPeriodeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandlingId);
        var periodeTidslinje = lagPeriodeTidslinje(ungdomsprogramPeriodeGrunnlag);
        var initiellPeriodeTidslinje = lagPeriodeTidslinje(initieltGrunnlag);
        var endretPerioderTidslinje = initiellPeriodeTidslinje.crossJoin(periodeTidslinje, UngdomsytelseVilkårsperioderTilVurderingTjeneste::erEndret)
            .filterValue(v -> v);
        return endretPerioderTidslinje.getLocalDateIntervals().stream().map(DatoIntervallEntitet::fra).collect(Collectors.toCollection(TreeSet::new));
    }

    private static LocalDateSegment<Boolean> erEndret(LocalDateInterval di, LocalDateSegment<Boolean> lhs, LocalDateSegment<Boolean> rhs) {
        return new LocalDateSegment<>(di, lhs == null || rhs == null || lhs.getValue().equals(rhs.getValue()));
    }

    private LocalDateTimeline<Boolean> lagPeriodeTidslinje(Optional<UngdomsprogramPeriodeGrunnlag> ungdomsprogramPeriodeGrunnlag) {
        return ungdomsprogramPeriodeGrunnlag.stream()
            .flatMap(gr -> gr.getUngdomsprogramPerioder().getPerioder().stream())
            .map(this::bestemPeriode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .map(this::komprimer)
            .orElse(LocalDateTimeline.empty());
    }

    private LocalDateTimeline<Boolean> komprimer(LocalDateTimeline<Boolean> t) {
        return t.compress((d1, d2) -> getKantIKantVurderer().erKantIKant(DatoIntervallEntitet.fra(d1), DatoIntervallEntitet.fra(d2)), Boolean::equals, StandardCombinators::alwaysTrueForMatch);
    }

    private DatoIntervallEntitet bestemPeriode(UngdomsprogramPeriode it) {
        DatoIntervallEntitet periode = it.getPeriode();
        // TOM dato fra register kan være null som mapper til tidenes ende. Men vi lar likevel vilkåret ha en enkel
        // maksgrense foreløpig
        if (periode.getTomDato().equals(AbstractLocalDateInterval.TIDENES_ENDE)) {
            return DatoIntervallEntitet.fraOgMedTilOgMed(
                periode.getFomDato(), periode.getFomDato().plus(PeriodeKonstanter.MAKS_PERIODE));
        }

        return periode;
    }
}
