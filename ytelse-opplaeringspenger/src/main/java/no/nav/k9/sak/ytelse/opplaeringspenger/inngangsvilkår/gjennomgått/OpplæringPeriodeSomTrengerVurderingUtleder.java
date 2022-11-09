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
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.UttaksPerioderGrunnlag;

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
        var institusjonsTidslinje = hentTidslinjeForVilkår(vilkårene, VilkårType.GODKJENT_OPPLÆRINGSINSTITUSJON);
        var sykdomsTidslinje = hentTidslinjeForVilkår(vilkårene, VilkårType.LANGVARIG_SYKDOM);

        var tidslinjeTilVurdering = TidslinjeUtil.tilTidslinjeKomprimert(perioderTilVurdering);
        var tidslinjeSomKreverVurdering = tidslinjeTilVurdering.intersection(institusjonsTidslinje).intersection(sykdomsTidslinje);

        return tidslinjeSomKreverVurdering
            .toSegments()
            .stream()
            .map(it -> DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    boolean trengerVurderingFraSaksbehandler(NavigableSet<DatoIntervallEntitet> perioderTilVurdering, Vilkårene vilkårene, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag, UttaksPerioderGrunnlag uttaksPerioderGrunnlag) {
        Objects.requireNonNull(uttaksPerioderGrunnlag);

        var perioderSomMåVurderes = utled(perioderTilVurdering, vilkårene);
        var tidslinjeSomMåVurderes = TidslinjeUtil.tilTidslinjeKomprimert(perioderSomMåVurderes);

        if (vurdertOpplæringGrunnlag != null && vurdertOpplæringGrunnlag.getVurdertePerioder() != null) {
            var vurdertTidslinje = vurdertOpplæringGrunnlag.getVurdertePerioder().getTidslinje();
            tidslinjeSomMåVurderes = tidslinjeSomMåVurderes.disjoint(vurdertTidslinje);
        }

        return !tidslinjeSomMåVurderes.isEmpty();
    }
}
