package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;

public class PerioderMedInaktivitetUtleder {

    public LocalDateTimeline<Boolean> utled(InaktivitetUtlederInput input) {
        var tidslinjeTilVurdering = input.getTidslinjeTilVurdering();

        if (tidslinjeTilVurdering.isEmpty()) {
            return LocalDateTimeline.EMPTY_TIMELINE;
        }

        var ikkeAktivTidslinje = new LocalDateTimeline<>(tidslinjeTilVurdering.toSegments()
            .stream()
            .map(it -> new LocalDateSegment<>(it.getLocalDateInterval(), true))
            .collect(Collectors.toList()));

        var aktørArbeid = input.getIayGrunnlag().getAktørArbeidFraRegister(input.getBrukerAktørId());

        var aktivTidslinje = aktørArbeid.map(this::mapAktørArbeid).orElse(new LocalDateTimeline<>(List.of()));

        return ikkeAktivTidslinje.disjoint(aktivTidslinje);
    }

    private LocalDateTimeline<Boolean> mapAktørArbeid(AktørArbeid aktørArbeid) {
        var arbeidsAktivTidslinje = new LocalDateTimeline<Boolean>(List.of());

        for (Yrkesaktivitet yrkesaktivitet : aktørArbeid.hentAlleYrkesaktiviteter()) {
            var segmenter = yrkesaktivitet.getAnsettelsesPeriode().stream()
                .map(it -> new LocalDateSegment<>(it.getPeriode().toLocalDateInterval(), true))
                .collect(Collectors.toList());
            // Har ikke helt kontroll på aa-reg mtp overlapp her så better safe than sorry
            for (LocalDateSegment<Boolean> segment : segmenter) {
                var arbeidsforholdTidslinje = new LocalDateTimeline<>(List.of(segment));
                arbeidsAktivTidslinje = arbeidsAktivTidslinje.combine(arbeidsforholdTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
            }
        }

        return arbeidsAktivTidslinje;
    }
}
