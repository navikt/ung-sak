package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

class OpplæringPeriodeSomTrengerVurderingUtleder {

    private static LocalDateTimeline<Boolean> hentTidslinjeForVilkår(Vilkårene vilkårene, VilkårType vilkårType) {
        var sykdomsVilkåret = vilkårene.getVilkår(vilkårType)
            .orElseThrow()
            .getPerioder()
            .stream()
            .filter(it -> Objects.equals(Utfall.OPPFYLT, it.getGjeldendeUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(TreeSet<DatoIntervallEntitet>::new));
        return TidslinjeUtil.tilTidslinjeKomprimert(sykdomsVilkåret);
    }

    NavigableSet<DatoIntervallEntitet> utled(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Vilkårene vilkårene) {
        var sykdomsTidslinje = hentTidslinjeForVilkår(vilkårene, VilkårType.LANGVARIG_SYKDOM);
        var nødvendighetsTidslinje = hentTidslinjeForVilkår(vilkårene, VilkårType.NØDVENDIG_OPPLÆRING);

        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);

        var tidslinjeSomIkkeKreverVurdering = tidslinjeTilVurdering.disjoint(sykdomsTidslinje)
            .disjoint(nødvendighetsTidslinje);

        return tidslinjeTilVurdering.disjoint(tidslinjeSomIkkeKreverVurdering)
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    boolean trengerVurderingFraSaksbehandler(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Vilkårene vilkårene) {
        var perioderSomMåVurderes = utled(perioderTilVurdering, vilkårene);

        // TODO: Fjern perioder som allerede har blitt vurdert

        return !perioderTilVurdering.isEmpty();
    }
}
