package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class InntektsmeldingFravær {

    public List<OppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Set<Inntektsmelding> inntektsmeldinger) {
        var sortedIm = inntektsmeldinger.stream().sorted(Inntektsmelding.COMP_REKKEFØLGE).collect(Collectors.toCollection(LinkedHashSet::new));
        
        var aktivitetType = UttakArbeidType.ARBEIDSTAKER;
        Map<Object, List<OppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var im : sortedIm) {
            var arbeidsgiver = im.getArbeidsgiver();
            var arbeidsforholdRef = im.getArbeidsforholdRef();
            var dummyGruppe = Arrays.asList(aktivitetType, arbeidsgiver, arbeidsforholdRef);
            var aktiviteter = mapByAktivitet.getOrDefault(dummyGruppe, new ArrayList<>());
            var liste = im.getOppgittFravær().stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), aktivitetType, arbeidsgiver, arbeidsforholdRef, pa.getVarighetPerDag()))
                .collect(Collectors.toList());

            var timeline = mapTilTimeline(aktiviteter);

            timeline = timeline.combine(mapTilTimeline(liste), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

            var oppdatertListe = timeline.compress()
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList());

            mapByAktivitet.put(dummyGruppe, oppdatertListe);
        }

        // sjekker mot overlappende data - foreløpig krasj and burn hvis overlappende segmenter
        for (var entry : mapByAktivitet.entrySet()) {
            var segments = entry.getValue().stream().map(ofp -> new LocalDateSegment<>(ofp.getFom(), ofp.getTom(), ofp)).collect(Collectors.toList());
            new LocalDateTimeline<>(segments);
        }
        return mapByAktivitet.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @NotNull
    private LocalDateTimeline<WrappedOppgittFraværPeriode> mapTilTimeline(List<OppgittFraværPeriode> aktiviteter) {
        return new LocalDateTimeline<>(aktiviteter.stream()
            .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom(), new WrappedOppgittFraværPeriode(it)))
            .collect(Collectors.toList()));
    }

    private OppgittFraværPeriode opprettHoldKonsistens(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        var value = segment.getValue().getPeriode();
        return new OppgittFraværPeriode(segment.getFom(), segment.getTom(), value.getAktivitetType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getFraværPerDag());
    }
}
