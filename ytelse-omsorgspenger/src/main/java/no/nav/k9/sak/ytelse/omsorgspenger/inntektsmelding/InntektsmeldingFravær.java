package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.util.ArrayList;
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
        Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<OppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var im : sortedIm) {
            var arbeidsgiver = im.getArbeidsgiver();
            var arbeidsforholdRef = im.getArbeidsforholdRef();
            var gruppe = new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(aktivitetType, new ArbeidsgiverArbeidsforhold(arbeidsgiver, arbeidsforholdRef));
            var aktiviteter = mapByAktivitet.getOrDefault(gruppe, new ArrayList<>());
            var liste = im.getOppgittFravær().stream()
                .map(pa -> new OppgittFraværPeriode(pa.getFom(), pa.getTom(), aktivitetType, arbeidsgiver, arbeidsforholdRef, pa.getVarighetPerDag()))
                .collect(Collectors.toList());

            var timeline = mapTilTimeline(aktiviteter);
            var imTidslinje = mapTilTimeline(liste);

            ryddOppIBerørteTidslinjer(mapByAktivitet, gruppe, imTidslinje);

            timeline = timeline.combine(imTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

            var oppdatertListe = timeline.compress()
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList());

            mapByAktivitet.put(gruppe, oppdatertListe);
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

    private void ryddOppIBerørteTidslinjer(Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<OppgittFraværPeriode>> mapByAktivitet,
                                           AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold gruppe,
                                           LocalDateTimeline<WrappedOppgittFraværPeriode> imTidslinje) {
        var entries = mapByAktivitet.entrySet()
            .stream()
            .filter(it -> !it.getKey().equals(gruppe) && it.getKey().gjelderSamme(gruppe))
            .collect(Collectors.toList());

        for (Map.Entry<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<OppgittFraværPeriode>> entry : entries) {
            var timeline = mapTilTimeline(entry.getValue());

            timeline = timeline.disjoint(imTidslinje);
            var oppdatertListe = timeline.compress()
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList());

            mapByAktivitet.put(entry.getKey(), oppdatertListe);
        }
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
