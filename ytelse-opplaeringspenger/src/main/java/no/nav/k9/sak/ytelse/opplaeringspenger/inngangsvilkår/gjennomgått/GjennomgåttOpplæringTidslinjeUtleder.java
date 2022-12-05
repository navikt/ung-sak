package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.VilkårTidslinjeUtleder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringTidslinjeUtleder {

    LocalDateTimeline<OpplæringGodkjenningStatus> utled(Vilkårene vilkårene, Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Objects.requireNonNull(vilkårene, "Vilkårene må være satt");
        Objects.requireNonNull(perioderFraSøknad, "Persioder fra søknad må være satt");
        Objects.requireNonNull(tidslinjeTilVurdering, "Tidslinje til vurdering må være satt");

        LocalDateTimeline<OpplæringGodkjenningStatus> tidslinjeIkkeGodkjentTidligereVilkår = lagTidslinjeMedIkkeGodkjentTidligereVilkår(vilkårene, tidslinjeTilVurdering);
        var tidslinjeTilVurderingEtterJusteringMotVilkår = tidslinjeTilVurdering.disjoint(tidslinjeIkkeGodkjentTidligereVilkår);

        LocalDateTimeline<OpplæringGodkjenningStatus> tidslinjeMedGjennomgåttOpplæring = lagTidslinjeGjennomgåttOpplæring(vurdertOpplæringGrunnlag, tidslinjeTilVurderingEtterJusteringMotVilkår);

        LocalDateTimeline<OpplæringGodkjenningStatus> tidslinjeReisetid = lagTidslinjeReisetid(perioderFraSøknad, vurdertOpplæringGrunnlag, tidslinjeTilVurderingEtterJusteringMotVilkår);

        return tidslinjeMedGjennomgåttOpplæring.crossJoin(tidslinjeReisetid).crossJoin(tidslinjeTilVurderingEtterJusteringMotVilkår.mapValue(value -> OpplæringGodkjenningStatus.MANGLER_VURDERING));
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> lagTidslinjeMedIkkeGodkjentTidligereVilkår(Vilkårene vilkårene, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var godkjentInstitusjonTidslinje = VilkårTidslinjeUtleder.utledOppfylt(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);
        var sykdomsTidslinje = VilkårTidslinjeUtleder.utledOppfylt(vilkårene, VilkårType.LANGVARIG_SYKDOM);

        var tidslinjeUtenGodkjentInstitusjon = tidslinjeTilVurdering.disjoint(godkjentInstitusjonTidslinje).mapValue(value -> OpplæringGodkjenningStatus.IKKE_GODKJENT_INSTITUSJON);
        var tidslinjeUtenSykdomsvilkår = tidslinjeTilVurdering.disjoint(sykdomsTidslinje).mapValue(value -> OpplæringGodkjenningStatus.IKKE_GODKJENT_SYKDOMSVILKÅR);

        return tidslinjeUtenGodkjentInstitusjon.crossJoin(tidslinjeUtenSykdomsvilkår);
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> lagTidslinjeGjennomgåttOpplæring(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            var vurdertTidslinje = vurdertOpplæringGrunnlag.getVurdertePerioder().getTidslinjeOpplæring().intersection(tidslinjeTilVurdering);

            var godkjentTidslinje = vurdertTidslinje.filterValue(VurdertOpplæringPeriode::getGjennomførtOpplæring).mapValue(value -> OpplæringGodkjenningStatus.GODKJENT);
            var ikkeGodkjentTidslinje = vurdertTidslinje.disjoint(godkjentTidslinje).mapValue(value -> OpplæringGodkjenningStatus.IKKE_GODKJENT);

            return godkjentTidslinje.crossJoin(ikkeGodkjentTidslinje);
        }

        return LocalDateTimeline.empty();
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> lagTidslinjeReisetid(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var oppgittReisetid = ReisetidUtleder.finnOppgittReisetid(perioderFraSøknad).mapValue(value -> OpplæringGodkjenningStatus.IKKE_GODKJENT_REISETID);
        var godkjentReisetid = ReisetidUtleder.utledGodkjentReisetid(perioderFraSøknad, vurdertOpplæringGrunnlag).mapValue(value -> OpplæringGodkjenningStatus.GODKJENT);

        return godkjentReisetid.crossJoin(oppgittReisetid).intersection(tidslinjeTilVurdering);
    }
}
