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
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.InstitusjonVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.OpplæringVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertInstitusjonHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;

public class NødvendighetVilkårOversetter {

    public NødvendighetVilkårGrunnlag oversettTilRegelModell(DatoIntervallEntitet periodeTilVurdering, VurdertOpplæringGrunnlag vurdertOpplæringGrunnlag) {
        Objects.requireNonNull(periodeTilVurdering);

        NavigableSet<DatoIntervallEntitet> godkjentInstitusjonPerioder = new TreeSet<>();
        NavigableSet<DatoIntervallEntitet> nødvendigOpplæringPerioder = new TreeSet<>();

        if (vurdertOpplæringGrunnlag != null) {
            List<VurdertOpplæring> vurdertOpplæringList = vurdertOpplæringGrunnlag.getVurdertOpplæringHolder().getVurdertOpplæring();
            VurdertInstitusjonHolder vurdertInstitusjonHolder = vurdertOpplæringGrunnlag.getVurdertInstitusjonHolder();

            for (VurdertOpplæring vurdertOpplæring : vurdertOpplæringList) {
                DatoIntervallEntitet datoIntervallEntitet = vurdertOpplæring.getPeriode();

                boolean godkjentInstitusjon = vurdertInstitusjonHolder.finnVurdertInstitusjon(vurdertOpplæring.getInstitusjon())
                    .map(VurdertInstitusjon::getGodkjent)
                    .orElse(false);

                if (godkjentInstitusjon) {
                    godkjentInstitusjonPerioder.add(datoIntervallEntitet);
                }

                boolean nødvendigOpplæring = vurdertOpplæring.getNødvendigOpplæring();

                if (nødvendigOpplæring) {
                    nødvendigOpplæringPerioder.add(datoIntervallEntitet);
                }
            }
        }

        LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(nødvendigOpplæringPerioder);
        LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(godkjentInstitusjonPerioder);

        LocalDateTimeline<OpplæringVurdering> nødvendigOpplæringVurderingTidslinje = nødvendigOpplæringTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), OpplæringVurdering.NØDVENDIG)));

        LocalDateTimeline<InstitusjonVurdering> godkjentInstitusjonVurderingTidslinje = godkjentInstitusjonTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), InstitusjonVurdering.GODKJENT)));

        LocalDateTimeline<OpplæringVurdering> opplæringVurderingTidslinje = new LocalDateTimeline<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), OpplæringVurdering.IKKE_NØDVENDIG)
            .combine(nødvendigOpplæringVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        LocalDateTimeline<InstitusjonVurdering> institusjonVurderingTidslinje = new LocalDateTimeline<>(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), InstitusjonVurdering.IKKE_GODKJENT)
            .combine(godkjentInstitusjonVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return new NødvendighetVilkårGrunnlag(periodeTilVurdering.getFomDato(), periodeTilVurdering.getTomDato(), opplæringVurderingTidslinje, institusjonVurderingTidslinje);
    }
}
