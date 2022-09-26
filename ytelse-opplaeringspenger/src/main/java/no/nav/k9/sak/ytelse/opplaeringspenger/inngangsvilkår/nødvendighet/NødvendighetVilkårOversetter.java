package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet;

import java.util.List;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.InstitusjonVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.OpplæringVurdering;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.nødvendighet.regelmodell.NødvendighetVilkårGrunnlag;

public class NødvendighetVilkårOversetter {

    public NødvendighetVilkårGrunnlag oversettTilRegelModell(DatoIntervallEntitet periode,
                                                             LocalDateTimeline<Boolean> nødvendigOpplæringTidslinje,
                                                             LocalDateTimeline<Boolean> godkjentInstitusjonTidslinje) {

        LocalDateTimeline<OpplæringVurdering> nødvendigOpplæringVurderingTidslinje = nødvendigOpplæringTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), OpplæringVurdering.NØDVENDIG)));

        LocalDateTimeline<InstitusjonVurdering> godkjentInstitusjonVurderingTidslinje = godkjentInstitusjonTidslinje
            .map(segment -> List.of(new LocalDateSegment<>(segment.getLocalDateInterval(), InstitusjonVurdering.GODKJENT)));

        LocalDateTimeline<OpplæringVurdering> relevantOpplæringVurderingTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), OpplæringVurdering.IKKE_NØDVENDIG)
            .combine(nødvendigOpplæringVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.LEFT_JOIN); //TODO denne kan være CROSS med nyeste regelimplementasjon

        LocalDateTimeline<InstitusjonVurdering> relevantInstitusjonVurderingTidslinje = new LocalDateTimeline<>(periode.getFomDato(), periode.getTomDato(), InstitusjonVurdering.IKKE_GODKJENT)
            .combine(godkjentInstitusjonVurderingTidslinje,
                StandardCombinators::coalesceRightHandSide,
                LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return new NødvendighetVilkårGrunnlag(periode.getFomDato(), periode.getTomDato(), relevantOpplæringVurderingTidslinje, relevantInstitusjonVurderingTidslinje);
    }
}
