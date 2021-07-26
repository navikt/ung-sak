package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class KravDokumentFravær {

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        var sorterteKravdokumenter = fraværFraKravdokumenter.keySet().stream().sorted().collect(Collectors.toCollection(LinkedHashSet::new));

        Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var dok : sorterteKravdokumenter) {
            var vurdertSøktPerioder = fraværFraKravdokumenter.get(dok);
            if (!vurdertSøktPerioder.isEmpty()) {
                var aktivitetTyper = vurdertSøktPerioder.stream().map(VurdertSøktPeriode::getType).collect(Collectors.toSet());
                for (var aktivitetType : aktivitetTyper) {
                    // TODO: Må håndtere søknad for flere arbeidsgivere og perioder. Må kunne sameksistere med IMs arbeidsforhold. Vurder om Dokumenttype må inkluderes her
                    var aktivitetGruppe = utledGruppe(vurdertSøktPerioder, aktivitetType);
                    var vurdertSøktPeriodePerAKtivitet = vurdertSøktPerioder.stream().filter(pa -> equalsGruppe(pa, aktivitetGruppe)).collect(Collectors.toList());

                    var aktiviteter = mapByAktivitet.getOrDefault(aktivitetGruppe, new ArrayList<>());
                    var aktivitetListe = vurdertSøktPeriodePerAKtivitet.stream()
                        .map(pa -> new WrappedOppgittFraværPeriode(pa.getRaw(), dok.getInnsendingsTidspunkt(), utledUtfall(pa)))
                        .collect(Collectors.toList());

                    var timeline = mapTilTimeline(aktiviteter);
                    var imTidslinje = mapTilTimeline(aktivitetListe);

                    ryddOppIBerørteTidslinjer(mapByAktivitet, aktivitetGruppe, imTidslinje);

                    timeline = timeline.combine(imTidslinje, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

                    var oppdatertAktivitetListe = timeline.compress()
                        .toSegments()
                        .stream()
                        .filter(it -> it.getValue() != null)
                        .filter(it -> it.getValue().getPeriode() != null)
                        .map(this::opprettHoldKonsistens)
                        .collect(Collectors.toList());

                    mapByAktivitet.put(aktivitetGruppe, oppdatertAktivitetListe);
                }
            }
        }

        // sjekker mot overlappende data - foreløpig krasj and burn hvis overlappende segmenter
        validerOverlapp(mapByAktivitet);
        return mapByAktivitet.values()
            .stream()
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private void ryddOppIBerørteTidslinjer(Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet,
                                           AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold gruppe,
                                           LocalDateTimeline<WrappedOppgittFraværPeriode> imTidslinje) {
        var entries = mapByAktivitet.entrySet()
            .stream()
            .filter(it -> !it.getKey().equals(gruppe) && it.getKey().gjelderSamme(gruppe))
            .collect(Collectors.toList());

        for (Map.Entry<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> entry : entries) {
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

    private boolean equalsGruppe(VurdertSøktPeriode<OppgittFraværPeriode> pa, AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold aktivitetGruppe) {
        return utledGruppe(List.of(pa), pa.getType()).equals(aktivitetGruppe);
    }

    private void validerOverlapp(Map<AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold, List<WrappedOppgittFraværPeriode>> mapByAktivitet) {
        mapByAktivitet.forEach((key, value) -> {
            var segments = value.stream().map(ofp -> new LocalDateSegment<>(ofp.getPeriode().getFom(), ofp.getPeriode().getTom(), ofp)).collect(Collectors.toList());
            new LocalDateTimeline<>(segments);
        });
    }

    private AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold utledGruppe(List<VurdertSøktPeriode<OppgittFraværPeriode>> vurdertSøktPerioder, UttakArbeidType aktivitetType) {
        Optional<Arbeidsgiver> arbeidsgiver = vurdertSøktPerioder.stream()
            .filter(søktPeriode -> søktPeriode.getType().equals(aktivitetType))
            .map(VurdertSøktPeriode::getArbeidsgiver)
            .filter(Objects::nonNull)
            .findFirst();
        if (arbeidsgiver.isPresent()) {
            var arbeidsforholdRef = vurdertSøktPerioder.stream().map(VurdertSøktPeriode::getArbeidsforholdRef).findFirst().orElseThrow();
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(aktivitetType, new ArbeidsgiverArbeidsforhold(arbeidsgiver.get(), arbeidsforholdRef));
        } else {
            return new AktivitetMedIdentifikatorArbeidsgiverArbeidsforhold(aktivitetType);
        }
    }

    private Utfall utledUtfall(VurdertSøktPeriode<OppgittFraværPeriode> pa) {
        if (Duration.ZERO.equals(pa.getRaw().getFraværPerDag())) {
            return Utfall.OPPFYLT;
        }
        return pa.getUtfall();
    }
}
