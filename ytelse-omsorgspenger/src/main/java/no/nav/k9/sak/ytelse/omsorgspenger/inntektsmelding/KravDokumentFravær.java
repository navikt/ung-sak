package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetIdentifikator.lagAktivitetIdentifikator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VurdertSøktPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;

public class KravDokumentFravær {

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        LinkedHashSet<KravDokument> sorterteKravdokumenter = sorterDokumenter(fraværFraKravdokumenter);

        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();
        for (var dok : sorterteKravdokumenter) {
            for (var vurdertPeriode : fraværFraKravdokumenter.get(dok)) {
                if (erImUtenRefusjonskravEllerTrektPeriode(dok.getType(), vurdertPeriode)) {
                    continue;
                }
                var aktivitetIdent = lagAktivitetIdentifikator(vurdertPeriode);

                var fraværsperiodeNy = new WrappedOppgittFraværPeriode(vurdertPeriode.getRaw(), dok.getInnsendingsTidspunkt(), dok.getType(), utledUtfall(vurdertPeriode));
                var fraværsperioderSammenslåtte = mapByAktivitet.getOrDefault(aktivitetIdent, new ArrayList<>());

                var tidslinjeNy = mapTilTimeline(List.of(fraværsperiodeNy));
                var tidslinjeSammenslått = mapTilTimeline(fraværsperioderSammenslåtte);

                mapByAktivitet = ryddOppIBerørteArbeidsforhold(mapByAktivitet, aktivitetIdent, tidslinjeNy);

                tidslinjeSammenslått = tidslinjeSammenslått.combine(tidslinjeNy, this::mergePeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);

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

    private LinkedHashSet<KravDokument> sorterDokumenter(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        return fraværFraKravdokumenter.keySet().stream()
            .sorted((kravDok1, kravDok2) -> {
                if (kravDok1.getType() != kravDok2.getType()) {
                    // Søknad har lavere pri enn Inntektsmelding og Fraværskorrrigering Inntektsmelding, må prosesseres først
                    return kravDok1.getType() == KravDokumentType.SØKNAD ? -1 : 1;
                }
                return kravDok1.getInnsendingsTidspunkt().compareTo(kravDok2.getInnsendingsTidspunkt());
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean erImUtenRefusjonskravEllerTrektPeriode(KravDokumentType type, VurdertSøktPeriode<OppgittFraværPeriode> vurdertPeriode) {
        var erTrektPeriode = vurdertPeriode.getRaw().getFraværPerDag() != null && vurdertPeriode.getRaw().getFraværPerDag().isZero();
        return type == KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV && !erTrektPeriode;
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergePeriode(LocalDateInterval di,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon,
                                                                       LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return sisteVersjon;
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return førsteVersjon;
        }
        var førstePeriode = førsteVersjon.getValue().getPeriode();
        var sistePeriode = sisteVersjon.getValue().getPeriode();

        var fraværÅrsak = sistePeriode.getFraværÅrsak() != null ? sistePeriode.getFraværÅrsak() : førstePeriode.getFraværÅrsak();
        var søknadÅrsak = sistePeriode.getSøknadÅrsak() != null ? sistePeriode.getSøknadÅrsak() : førstePeriode.getSøknadÅrsak();

        // TODO: Ta i bruk konfliktImSøknad i Uttak
        boolean konfliktImSøknad = erKonfliktMellomImOgSøknad(førsteVersjon, sisteVersjon);

        // Siste segment for alt annet brukes alltid siste segment
        var gjeldendeFp = sisteVersjon.getValue().getPeriode();
        var wrapped = new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(gjeldendeFp.getJournalpostId(),
            di.getTomDato(),
            di.getTomDato(),
            gjeldendeFp.getAktivitetType(),
            gjeldendeFp.getArbeidsgiver(),
            gjeldendeFp.getArbeidsforholdRef(),
            gjeldendeFp.getFraværPerDag(),
            fraværÅrsak,
            søknadÅrsak),
            sisteVersjon.getValue().getInnsendingstidspunkt(),
            sisteVersjon.getValue().getKravDokumentType(),
            sisteVersjon.getValue().getSøknadsfristUtfall());

        return new LocalDateSegment<>(di, wrapped);
    }

    private boolean erKonfliktMellomImOgSøknad(LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon, LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {

        WrappedOppgittFraværPeriode im;
        WrappedOppgittFraværPeriode søknad;
        if (førsteVersjon.getValue().getKravDokumentType() != sisteVersjon.getValue().getKravDokumentType()) {
            if (førsteVersjon.getValue().getKravDokumentType() == KravDokumentType.INNTEKTSMELDING_MED_REFUSJONSKRAV && sisteVersjon.getValue().getKravDokumentType() == KravDokumentType.SØKNAD) {
                im = førsteVersjon.getValue();
                søknad = sisteVersjon.getValue();
            } else if (sisteVersjon.getValue().getKravDokumentType() == KravDokumentType.SØKNAD && førsteVersjon.getValue().getKravDokumentType() == KravDokumentType.INNTEKTSMELDING_MED_REFUSJONSKRAV) {
                søknad = førsteVersjon.getValue();
                im = sisteVersjon.getValue();
            } else {
                return false;
            }

            var erForskjellFravær = Objects.equals(im.getPeriode().getFraværPerDag(), søknad.getPeriode().getFraværPerDag());
            var erTrekkAvKrav = im.getPeriode().getFraværPerDag() != null && im.getPeriode().getFraværPerDag().isZero();
            if (erForskjellFravær && !erTrekkAvKrav) {
                return true;
            }

            if (im.getKravDokumentType() == KravDokumentType.INNTEKTSMELDING_MED_REFUSJONSKRAV) {
                return true;
            }
        }
        return false;
    }

    /**
     * Rydd opp i arbeidsforhold for samme arbeidsgiver, men annet arbeidsforhold
     */
    private Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> ryddOppIBerørteArbeidsforhold(
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> mapByAktivitet,
        AktivitetIdentifikator aktivitetIdent,
        LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNye) {

        var entriesBerørteArbeidsforhold = mapByAktivitet.entrySet()
            .stream()
            // Samme arbeidsgiver, men annet arbeidsforhold
            .filter(it -> !it.getKey().equals(aktivitetIdent) && it.getKey().gjelderSamme(aktivitetIdent))
            .collect(Collectors.toList());

        for (Map.Entry<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> entry : entriesBerørteArbeidsforhold) {
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
        return new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(value.getJournalpostId(), segment.getFom(), segment.getTom(), value.getAktivitetType(), value.getArbeidsgiver(), value.getArbeidsforholdRef(), value.getFraværPerDag(), value.getFraværÅrsak(), value.getSøknadÅrsak()),
            segment.getValue().getInnsendingstidspunkt(),
            segment.getValue().getKravDokumentType(),
            segment.getValue().getSøknadsfristUtfall());
    }


    private void validerOverlapp(Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> mapByAktivitet) {
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
