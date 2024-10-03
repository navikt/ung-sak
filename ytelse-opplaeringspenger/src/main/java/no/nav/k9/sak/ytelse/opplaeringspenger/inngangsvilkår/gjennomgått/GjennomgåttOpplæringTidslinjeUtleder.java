package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import java.util.Objects;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.VilkårTidslinjeUtleder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringPeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;

class GjennomgåttOpplæringTidslinjeUtleder {

    LocalDateTimeline<OpplæringGodkjenningStatus> utled(Vilkårene vilkårene, Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        Objects.requireNonNull(vilkårene, "Vilkårene må være satt");
        Objects.requireNonNull(perioderFraSøknad, "Perioder fra søknad må være satt");
        Objects.requireNonNull(tidslinjeTilVurdering, "Tidslinje til vurdering må være satt");

        var manglendePerioder = lagTidslinjeMedMangledePerioderFraTidligereVilkår(vilkårene, tidslinjeTilVurdering);
        var oppdatertTidslinjeTilVurdering = tidslinjeTilVurdering.combine(manglendePerioder, StandardCombinators::alwaysTrueForMatch, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var tidslinjeIkkeGodkjentTidligereVilkår = lagTidslinjeMedIkkeGodkjentTidligereVilkår(vilkårene);
        var tidslinjeTilVurderingEtterJusteringMotVilkår = oppdatertTidslinjeTilVurdering.disjoint(tidslinjeIkkeGodkjentTidligereVilkår);

        LocalDateTimeline<OpplæringGodkjenningStatus> tidslinjeMedGjennomgåttOpplæring = lagTidslinjeGjennomgåttOpplæring(vurdertOpplæringGrunnlag, tidslinjeTilVurderingEtterJusteringMotVilkår);

        LocalDateTimeline<OpplæringGodkjenningStatus> tidslinjeReisetid = lagTidslinjeReisetid(perioderFraSøknad, vurdertOpplæringGrunnlag, tidslinjeTilVurderingEtterJusteringMotVilkår);

        return tidslinjeMedGjennomgåttOpplæring.crossJoin(tidslinjeReisetid).crossJoin(Hjelpetidslinjer.fjernHelger(tidslinjeTilVurderingEtterJusteringMotVilkår).mapValue(value -> OpplæringGodkjenningStatus.MANGLER_VURDERING_OPPLÆRING));
    }

    private LocalDateTimeline<Boolean> lagTidslinjeMedMangledePerioderFraTidligereVilkår(Vilkårene vilkårene, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var godkjentePerioderForGjennomførtOpplæring = VilkårTidslinjeUtleder.utledOppfylt(vilkårene, VilkårType.LANGVARIG_SYKDOM);

        return godkjentePerioderForGjennomførtOpplæring
            .disjoint(tidslinjeTilVurdering);
    }

    private LocalDateTimeline<Boolean> lagTidslinjeMedIkkeGodkjentTidligereVilkår(Vilkårene vilkårene) {
        var tidslinjeUtenGodkjentInstitusjon = VilkårTidslinjeUtleder.utledAvslått(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);
        var tidslinjeUtenSykdomsvilkår = VilkårTidslinjeUtleder.utledAvslått(vilkårene, VilkårType.LANGVARIG_SYKDOM);

        return tidslinjeUtenGodkjentInstitusjon.crossJoin(tidslinjeUtenSykdomsvilkår);
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> lagTidslinjeGjennomgåttOpplæring(VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            var vurdertTidslinje = vurdertOpplæringGrunnlag.getVurdertePerioder().getTidslinjeOpplæring().intersection(tidslinjeTilVurdering);

            var godkjentTidslinje = vurdertTidslinje.filterValue(VurdertOpplæringPeriode::getGjennomførtOpplæring).mapValue(value -> OpplæringGodkjenningStatus.GODKJENT);
            var ikkeGodkjentTidslinje = vurdertTidslinje.disjoint(godkjentTidslinje).mapValue(value -> OpplæringGodkjenningStatus.IKKE_GODKJENT_OPPLÆRING);

            return godkjentTidslinje.crossJoin(ikkeGodkjentTidslinje);
        }

        return LocalDateTimeline.empty();
    }

    private LocalDateTimeline<OpplæringGodkjenningStatus> lagTidslinjeReisetid(Set<PerioderFraSøknad> perioderFraSøknad, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var oppgittReisetid = ReisetidUtleder.finnOppgittReisetid(perioderFraSøknad).mapValue(value -> OpplæringGodkjenningStatus.MANGLER_VURDERING_REISETID);
        var vurdertReisetid = ReisetidUtleder.utledVurdertReisetid(perioderFraSøknad, vurdertOpplæringGrunnlag);

        return vurdertReisetid.crossJoin(oppgittReisetid).intersection(tidslinjeTilVurdering);
    }
}
