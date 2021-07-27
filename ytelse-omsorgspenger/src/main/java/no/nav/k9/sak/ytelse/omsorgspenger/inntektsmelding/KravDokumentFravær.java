package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold.lagAktivitetIdentifikator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class KravDokumentFravær {

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        var sorterteKravdokumenter = fraværFraKravdokumenter.keySet().stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));

        Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var dok : sorterteKravdokumenter) {
            var vurdertePerioder = fraværFraKravdokumenter.get(dok);
            var vurdertePerioderMedAktivitetIdent = vurdertePerioder.stream()
                .collect(Collectors.toMap(e -> e, e -> lagAktivitetIdentifikator(e)));

            for (var entry : vurdertePerioderMedAktivitetIdent.entrySet()) {
                var vurdertPeriode = entry.getKey();
                var aktivitetIdent = entry.getValue();

                var fraværsperiodeNy = new WrappedOppgittFraværPeriode(vurdertPeriode.getRaw(), dok.getInnsendingsTidspunkt(), utledUtfall(vurdertPeriode));
                var fraværsperioderSammenslåtte = mapByAktivitet.getOrDefault(aktivitetIdent, new ArrayList<>());

                var tidslinjeNy = mapTilTimeline(List.of(fraværsperiodeNy));
                var tidslinjeSammenslått = mapTilTimeline(fraværsperioderSammenslåtte);

                mapByAktivitet = ryddOppIBerørteArbeidsforhold(mapByAktivitet, aktivitetIdent, tidslinjeNy);

                tidslinjeSammenslått = tidslinjeSammenslått.combine(tidslinjeNy, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

                var oppdatertAktivitetListe = tidslinjeSammenslått.compress()
                    .toSegments()
                    .stream()
                    .filter(it -> it.getValue() != null)
                    .filter(it -> it.getValue().getPeriode() != null)
                    .map(this::opprettHoldKonsistens)
                    .collect(Collectors.toList());

                mapByAktivitet.put(aktivitetIdent, oppdatertAktivitetListe);
            }
        }

        // sjekker mot overlappende data - foreløpig krasj and burn hvis overlappende segmenter
        validerOverlapp(mapByAktivitet);
        return mapByAktivitet.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    /**
     * Rydd opp i arbeidsforhold for samme arbeidsgiver, men annet arbeidsforhold
     */
    private Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> ryddOppIBerørteArbeidsforhold(
        Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet,
        AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold aktivitetIdent,
        LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNye) {

        var entriesBerørteArbeidsforhold = mapByAktivitet.entrySet()
            .stream()
            // Samme arbeidsgiver, men annet arbeidsforhold
            .filter(it -> !it.getKey().equals(aktivitetIdent) && it.getKey().gjelderSamme(aktivitetIdent))
            .collect(Collectors.toList());

        for (Map.Entry<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> entry : entriesBerørteArbeidsforhold) {
            var tidslinjeBerørt = mapTilTimeline(entry.getValue());

            tidslinjeBerørt = tidslinjeBerørt.disjoint(tidslinjeNye);
            var oppdatertListe = tidslinjeBerørt.compress()
                .toSegments()
                .stream()
                .filter(it -> it.getValue() != null)
                .filter(it -> it.getValue().getPeriode() != null)
                .map(this::opprettHoldKonsistens)
                .collect(Collectors.toList());

            mapByAktivitet.put(entry.getKey(), oppdatertListe);
        }
        return mapByAktivitet;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mapTilTimeline(List<WrappedOppgittFraværPeriode> aktiviteter) {
        return new LocalDateTimeline<>(aktiviteter.stream()
            .map(it -> new LocalDateSegment<>(it.getPeriode().getFom(), it.getPeriode().getTom(), it))
            .collect(Collectors.toList()));
    }

    private WrappedOppgittFraværPeriode opprettHoldKonsistens(LocalDateSegment<WrappedOppgittFraværPeriode> segment) {
        var value = segment.getValue().getPeriode();
        return new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(value.getJournalpostId(), segment.getFom(), segment.getTom(), value.getAktivitetType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getFraværPerDag(), value.getFraværÅrsak()),
            segment.getValue().getInnsendingstidspunkt(),
            segment.getValue().getSøknadsfristUtfall());
    }


    private void validerOverlapp(Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet) {
        mapByAktivitet.forEach((key, value) -> {
            var segments = value.stream().map(ofp -> new LocalDateSegment<>(ofp.getPeriode().getFom(), ofp.getPeriode().getTom(), ofp)).collect(Collectors.toList());
            new LocalDateTimeline<>(segments);
        });
    }

    private Utfall utledUtfall(VurdertSøktPeriode<OppgittFraværPeriode> pa) {
        if (Duration.ZERO.equals(pa.getRaw().getFraværPerDag())) {
            return Utfall.OPPFYLT;
        }
        return pa.getUtfall();
    }
}
