package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.OpplæringVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;

public class NødvendighetVilkårOversetter {

    public NødvendighetVilkårGrunnlag oversettTilRegelModell(DatoIntervallEntitet periodeTilVurdering, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        Objects.requireNonNull(periodeTilVurdering);
        Objects.requireNonNull(vurdertOpplæringGrunnlag);

        NavigableSet<DatoIntervallEntitet> nødvendigOpplæringPerioder = new TreeSet<>();

        List<VurdertOpplæring> vurdertOpplæringList = vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring();

        for (VurdertOpplæring vurdertOpplæring : vurdertOpplæringList) {
            DatoIntervallEntitet datoIntervallEntitet = vurdertOpplæring.getPeriode();

            boolean nødvendigOpplæring = vurdertOpplæring.getNødvendigOpplæring();

            if (nødvendigOpplæring) {
                nødvendigOpplæringPerioder.add(datoIntervallEntitet);
            }
        }

        LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(nødvendigOpplæringPerioder);

        LocalDateTimeline<OpplæringVurdering> nødvendigOpplæringVurderingTidslinje = nødvendigOpplæringTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), OpplæringVurdering.NØDVENDIG)));

        LocalDateTimeline<OpplæringVurdering> opplæringVurderingTidslinje = new LocalDateTimeline<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), OpplæringVurdering.IKKE_NØDVENDIG)
            .combine(nødvendigOpplæringVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return new NødvendighetVilkårGrunnlag(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), opplæringVurderingTidslinje);
    }
}
