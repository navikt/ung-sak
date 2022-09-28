package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.InstitusjonVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.OpplæringVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.SykdomVurdering;

public class NødvendighetVilkårOversetter {

    public NødvendighetVilkårGrunnlag oversettTilRegelModell(DatoIntervallEntitet periode,
                                                             LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje,
                                                             LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje,
                                                             LocalDateTimeline<Boolean> godkjentSykdomsvilkårTidslinje) {

        LocalDateTimeline<OpplæringVurdering> nødvendigOpplæringVurderingTidslinje = nødvendigOpplæringTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), OpplæringVurdering.NØDVENDIG)));

        LocalDateTimeline<InstitusjonVurdering> godkjentInstitusjonVurderingTidslinje = godkjentInstitusjonTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), InstitusjonVurdering.GODKJENT)));

        LocalDateTimeline<SykdomVurdering> godkjentSykdomVurderingTidslinje = godkjentSykdomsvilkårTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), SykdomVurdering.GODKJENT)));

        LocalDateTimeline<OpplæringVurdering> opplæringVurderingTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), OpplæringVurdering.IKKE_NØDVENDIG)
            .combine(nødvendigOpplæringVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        LocalDateTimeline<InstitusjonVurdering> institusjonVurderingTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), InstitusjonVurdering.IKKE_GODKJENT)
            .combine(godkjentInstitusjonVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        LocalDateTimeline<SykdomVurdering> sykdomVurderingTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), SykdomVurdering.IKKE_GODKJENT)
            .combine(godkjentSykdomVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.CROSS_JOIN);

        return new NødvendighetVilkårGrunnlag(periode.getFomDato(), periode.getTomDato(), opplæringVurderingTidslinje, institusjonVurderingTidslinje, sykdomVurderingTidslinje);
    }
}
