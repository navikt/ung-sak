package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import static no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding.AktivitetIdentifikator.lagAktivitetIdentifikator;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    private static final Set<KravDokumentType> SØKNAD_TYPER = Set.of(KravDokumentType.SØKNAD);
    private static final Set<KravDokumentType> IM_TYPER = Set.of(KravDokumentType.INNTEKTSMELDING, KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV);

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {

        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderSøknad = utledFraværsperioder(fraværFraKravdokumenter, SØKNAD_TYPER);
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderIm = utledFraværsperioder(fraværFraKravdokumenter, IM_TYPER);

        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått = slåSammenSøknadOgInntektsmelding(fraværsperioderSøknad, fraværsperioderIm);

        validerOverlapp(fraværsperioderSammenslått);
        return fraværsperioderSammenslått.values().stream().flatMap(Collection::stream).toList();
    }

    private Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> utledFraværsperioder(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter, Set<KravDokumentType> kravdokumentTyper) {
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();

        fraværFraKravdokumenter.entrySet().stream()
            .filter(e -> kravdokumentTyper.contains(e.getKey().getType()))
            .sorted(Comparator.comparing(e -> e.getKey().getInnsendingsTidspunkt()))
            .forEachOrdered(e -> {
                KravDokument dok = e.getKey();
                for (VurdertSøktPeriode<OppgittFraværPeriode> vurdertPeriode : e.getValue()) {
                    if (erImUtenRefusjonskravOgUtenTrektPeriode(dok.getType(), vurdertPeriode)) {
                        continue;
                    }
                    var aktivitetIdent = lagAktivitetIdentifikator(vurdertPeriode);

                    var fraværsperiodeNy = new WrappedOppgittFraværPeriode(vurdertPeriode.getRaw(), dok.getInnsendingsTidspunkt(), dok.getType(), utledUtfall(vurdertPeriode));
                    var fraværsperioderSammenslåtte = mapByAktivitet.getOrDefault(aktivitetIdent, new ArrayList<>());

                    var tidslinjeNy = mapTilTimeline(List.of(fraværsperiodeNy));
                    var tidslinjeSammenslått = mapTilTimeline(fraværsperioderSammenslåtte);

                    ryddOppIBerørteArbeidsforhold(mapByAktivitet, aktivitetIdent, tidslinjeNy);

                    mapByAktivitet.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeNy, tidslinjeSammenslått));
                }
            });

        return mapByAktivitet;
    }

    private List<WrappedOppgittFraværPeriode> slåSammenTidslinjer(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNy, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSammenslått) {

        tidslinjeSammenslått = tidslinjeSammenslått.combine(tidslinjeNy, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        var oppdatertAktivitetListe = tidslinjeSammenslått.compress()
            .toSegments()
            .stream()
            .map(this::opprettHoldKonsistens)
            .collect(Collectors.toList());

        return oppdatertAktivitetListe;
    }

    private Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> slåSammenSøknadOgInntektsmelding(
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderIm) {

        // Begynner med fraværsperioder fra søknad som seed, før merge med fraværsperioder fra inntektsmelding
        Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått = new LinkedHashMap<>(fraværsperioderSøknad);

        for (Map.Entry<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> entryIm : fraværsperioderIm.entrySet()) {
            var aktivitetIdent = entryIm.getKey();
            var tidslinjeSøknad = finnSøknadTidslinje(fraværsperioderSøknad, aktivitetIdent);
            for (var fraværPeriodeIm : entryIm.getValue()) {
                var fraværsperioder = fraværsperioderSammenslått.getOrDefault(aktivitetIdent, new ArrayList<>());
                var tidslinjeIm = mapTilTimeline(List.of(fraværPeriodeIm));
                tidslinjeIm = leggPåSøknadsårsakerFraSøknad(tidslinjeIm, tidslinjeSøknad);

                ryddOppIBerørteArbeidsforhold(fraværsperioderSammenslått, aktivitetIdent, tidslinjeIm);

                var tidslinjeSammenslått = mapTilTimeline(fraværsperioder);
                fraværsperioderSammenslått.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeIm, tidslinjeSammenslått));
            }
        }
        return fraværsperioderSammenslått;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> leggPåSøknadsårsakerFraSøknad(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeIm, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSøknad) {
        var tidslinjeImMedSøknadÅrsaker = tidslinjeIm.combine(tidslinjeSøknad, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        var fraværPerioderImMedSøknadsårsaker = tidslinjeImMedSøknadÅrsaker.compress()
            .toSegments()
            .stream()
            .map(this::opprettHoldKonsistens)
            .collect(Collectors.toList());

        return mapTilTimeline(fraværPerioderImMedSøknadsårsaker);
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> finnSøknadTidslinje(Map<AktivitetIdentifikator, List<WrappedOppgittFraværPeriode>> fraværsperioderSøknad, AktivitetIdentifikator aktivitetIdent) {
        return fraværsperioderSøknad.entrySet().stream()
            .filter(e -> e.getKey().gjelderSamme(aktivitetIdent)) //kan få maksimalt én match siden søknad ikke oppgir arbeidsforholdId
            .map(Map.Entry::getValue)
            .map(this::mapTilTimeline)
            .findAny()
            .orElse((LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
    }

    private boolean erImUtenRefusjonskravOgUtenTrektPeriode(KravDokumentType type, VurdertSøktPeriode<OppgittFraværPeriode> vurdertPeriode) {
        var erTrektPeriode = vurdertPeriode.getRaw().getFraværPerDag() != null && vurdertPeriode.getRaw().getFraværPerDag().isZero();
        return type == KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV && !erTrektPeriode;
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> oppdaterPeriode(LocalDateInterval di,
                                                                          LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon,
                                                                          LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {
        if (førsteVersjon == null && sisteVersjon != null) {
            return sisteVersjon;
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return førsteVersjon;
        }
        var førsteWrapped = førsteVersjon.getValue();
        var sisteWrapped = sisteVersjon.getValue();
        if (IM_TYPER.contains(førsteWrapped.getKravDokumentType()) && SØKNAD_TYPER.contains(sisteWrapped.getKravDokumentType())) {
            return mergeImMedSøknad(di, førsteWrapped, sisteWrapped);
        } else if (SØKNAD_TYPER.contains(førsteWrapped.getKravDokumentType()) && IM_TYPER.contains(sisteWrapped.getKravDokumentType())) {
            return mergeImMedSøknad(di, sisteWrapped, førsteWrapped);
        } else {
            return oppdaterPeriode(di, sisteWrapped);
        }
    }


    private LocalDateSegment<WrappedOppgittFraværPeriode> oppdaterPeriode(LocalDateInterval di, WrappedOppgittFraværPeriode fraværPeriode) {
        var wrapped = new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(fraværPeriode.getPeriode().getJournalpostId(),
            di.getTomDato(),
            di.getTomDato(),
            fraværPeriode.getPeriode().getAktivitetType(),
            fraværPeriode.getPeriode().getArbeidsgiver(),
            fraværPeriode.getPeriode().getArbeidsforholdRef(),
            fraværPeriode.getPeriode().getFraværPerDag(),
            fraværPeriode.getPeriode().getFraværÅrsak(),
            fraværPeriode.getPeriode().getSøknadÅrsak()),
            fraværPeriode.getInnsendingstidspunkt(),
            fraværPeriode.getKravDokumentType(),
            fraværPeriode.getSøknadsfristUtfall());

        return new LocalDateSegment<>(di, wrapped);
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergeImMedSøknad(LocalDateInterval di, WrappedOppgittFraværPeriode im, WrappedOppgittFraværPeriode søknad) {
        var gjeldende = søknadHarKravOgImHarTrektKrav(im, søknad) ? søknad : im;
        // TODO: Ta i bruk konfliktImSøknad i Uttak
        boolean konfliktImSøknad = erAvvikMellomImOgSøknad(im, søknad);

        var wrapped = new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(gjeldende.getPeriode().getJournalpostId(),
            di.getTomDato(),
            di.getTomDato(),
            gjeldende.getPeriode().getAktivitetType(),
            gjeldende.getPeriode().getArbeidsgiver(),
            gjeldende.getPeriode().getArbeidsforholdRef(),
            gjeldende.getPeriode().getFraværPerDag(),
            søknad.getPeriode().getFraværÅrsak(),
            søknad.getPeriode().getSøknadÅrsak()),
            gjeldende.getInnsendingstidspunkt(),
            gjeldende.getKravDokumentType(),
            gjeldende.getSøknadsfristUtfall());

        return new LocalDateSegment<>(di, wrapped);
    }

    private boolean søknadHarKravOgImHarTrektKrav(WrappedOppgittFraværPeriode im, WrappedOppgittFraværPeriode søknad) {
        var harSøknadKrav = søknad.getPeriode().getFraværPerDag() == null || !søknad.getPeriode().getFraværPerDag().isZero();
        var erImTrekkAvKrav = im.getPeriode().getFraværPerDag() != null && im.getPeriode().getFraværPerDag().isZero();
        return harSøknadKrav && erImTrekkAvKrav;
    }

    private boolean erAvvikMellomImOgSøknad(WrappedOppgittFraværPeriode im, WrappedOppgittFraværPeriode søknad) {
        var erForskjellFravær = Objects.equals(im.getPeriode().getFraværPerDag(), søknad.getPeriode().getFraværPerDag());
        var erImTrekkAvKrav = im.getPeriode().getFraværPerDag() != null && im.getPeriode().getFraværPerDag().isZero();
        return erForskjellFravær && !erImTrekkAvKrav;
    }

    /**
     * Rydd opp i arbeidsforhold for samme arbeidsgiver, men annet arbeidsforhold
     */
    private void ryddOppIBerørteArbeidsforhold(
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
        // LocalDateTimeline kaster exception dersom overlapp i perioder
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
