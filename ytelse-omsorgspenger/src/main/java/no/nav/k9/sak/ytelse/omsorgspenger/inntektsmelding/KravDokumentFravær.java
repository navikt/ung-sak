package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
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

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad = utledFraværsperioder(fraværFraKravdokumenter, SØKNAD_TYPER);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderIm = utledFraværsperioder(fraværFraKravdokumenter, IM_TYPER);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått = slåSammenSøknadOgInntektsmelding(fraværsperioderSøknad, fraværsperioderIm);

        return fraværsperioderSammenslått.values().stream()
            .map(KravDokumentFravær::compress)
            .flatMap(LocalDateTimeline::stream)
            .map(LocalDateSegment::getValue)
            .toList();
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> utledFraværsperioder(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter, Set<KravDokumentType> kravdokumentTyper) {
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();

        fraværFraKravdokumenter.entrySet().stream()
            .filter(e -> kravdokumentTyper.contains(e.getKey().getType()))
            .sorted(Comparator.comparing(e -> e.getKey().getInnsendingsTidspunkt()))
            .forEachOrdered(dokumentEntry -> {
                KravDokument dok = dokumentEntry.getKey();
                var fraværPerioder = dokumentEntry.getValue();
                fraværPerioder.stream()
                    .filter(vurdertPeriode -> !erImUtenRefusjonskravOgUtenTrektPeriode(dok.getType(), vurdertPeriode))
                    .collect(Collectors.groupingBy(AktivitetIdentifikator::lagAktivitetIdentifikator))
                    .forEach((aktivitetIdent, aktivitetFraværPerioder) -> {
                        var tidslinjeNy = mapTilTimeline(aktivitetFraværPerioder.stream().map(v -> new WrappedOppgittFraværPeriode(v.getRaw(), dok.getInnsendingsTidspunkt(), dok.getType(), utledUtfall(v))).toList());
                        var tidslinjeSammenslått = mapByAktivitet.getOrDefault(aktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
                        ryddOppIBerørteArbeidsforhold(mapByAktivitet, aktivitetIdent, tidslinjeNy);
                        mapByAktivitet.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinjeNy));
                    });
            });

        return mapByAktivitet;
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> slåSammenSøknadOgInntektsmelding(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderIm) {

        // Begynner med fraværsperioder fra søknad som seed, før merge med fraværsperioder fra inntektsmelding
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått = new LinkedHashMap<>(fraværsperioderSøknad);

        for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> entryIm : fraværsperioderIm.entrySet()) {
            var aktivitetIdent = entryIm.getKey();
            var tidslinjeIm = entryIm.getValue();
            var tidslinjeSøknad = finnSøknadTidslinje(fraværsperioderSøknad, aktivitetIdent);
            var tidslinjeSammenslått = fraværsperioderSammenslått.getOrDefault(aktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
            tidslinjeIm = leggPåSøknadsårsakerFraSøknad(tidslinjeIm, tidslinjeSøknad);
            ryddOppIBerørteArbeidsforhold(fraværsperioderSammenslått, aktivitetIdent, tidslinjeIm);
            fraværsperioderSammenslått.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinjeIm));
        }
        return fraværsperioderSammenslått;
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> slåSammenTidslinjer(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSammenslått, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNy) {
        return tidslinjeSammenslått.combine(tidslinjeNy, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> leggPåSøknadsårsakerFraSøknad(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeIm, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSøknad) {
        return tidslinjeIm.combine(tidslinjeSøknad, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> finnSøknadTidslinje(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad, AktivitetIdentifikator aktivitetIdent) {
        return fraværsperioderSøknad.entrySet().stream()
            .filter(e -> e.getKey().gjelderSamme(aktivitetIdent)) //kan få maksimalt én match siden søknad ikke oppgir arbeidsforholdId
            .map(Map.Entry::getValue)
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
            return nyPeriode(di, sisteVersjon);
        } else if (sisteVersjon == null && førsteVersjon != null) {
            return nyPeriode(di, førsteVersjon);
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
            di.getFomDato(),
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

        var wrapped = new WrappedOppgittFraværPeriode(new OppgittFraværPeriode(gjeldende.getPeriode().getJournalpostId(),
            di.getFomDato(),
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

    /**
     * Rydd opp i arbeidsforhold for samme arbeidsgiver, men annet arbeidsforhold
     */
    private void ryddOppIBerørteArbeidsforhold(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> mapByAktivitet,
        AktivitetIdentifikator aktivitetIdent,
        LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNye) {

        var entriesBerørteArbeidsforhold = mapByAktivitet.entrySet()
            .stream()
            // Samme arbeidsgiver, men annet arbeidsforhold
            .filter(it -> !it.getKey().equals(aktivitetIdent) && it.getKey().gjelderSamme(aktivitetIdent))
            .toList();

        for (Map.Entry<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> entry : entriesBerørteArbeidsforhold) {
            var tidslinjeBerørt = entry.getValue();
            tidslinjeBerørt = tidslinjeBerørt.disjoint(tidslinjeNye, KravDokumentFravær::leftOnly);
            mapByAktivitet.put(entry.getKey(), tidslinjeBerørt);
        }
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mapTilTimeline(Collection<WrappedOppgittFraværPeriode> aktiviteter) {
        return new LocalDateTimeline<>(aktiviteter.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFom(), a.getPeriode().getTom(), a))
            .toList());
    }

    private static LocalDateSegment<WrappedOppgittFraværPeriode> nyPeriode(LocalDateInterval nyPeriode, LocalDateSegment<WrappedOppgittFraværPeriode> gjeldende) {
        return new LocalDateSegment<>(nyPeriode, kloneVerdierNyPeriode(gjeldende.getValue(), nyPeriode.getFomDato(), nyPeriode.getTomDato()));
    }

    public static LocalDateSegment<WrappedOppgittFraværPeriode> leftOnly(LocalDateInterval dateInterval, LocalDateSegment<WrappedOppgittFraværPeriode> lhs, LocalDateSegment<WrappedOppgittFraværPeriode> rhs) {
        return new LocalDateSegment<>(dateInterval, kloneVerdierNyPeriode(lhs.getValue(), dateInterval.getFomDato(), dateInterval.getTomDato()));
    }

    public static LocalDateTimeline<WrappedOppgittFraværPeriode> compress(LocalDateTimeline<WrappedOppgittFraværPeriode> timeline) {
        BiPredicate<WrappedOppgittFraværPeriode, WrappedOppgittFraværPeriode> equalsChecker = (WrappedOppgittFraværPeriode v1, WrappedOppgittFraværPeriode v2) -> {
            return Objects.equals(v1.getKravDokumentType(), v2.getKravDokumentType())
                && Objects.equals(v1.getInnsendingstidspunkt(), v2.getInnsendingstidspunkt())
                && Objects.equals(v1.getSøknadsfristUtfall(), v2.getSøknadsfristUtfall())
                && Objects.equals(v1.getPeriode().getJournalpostId(), v2.getPeriode().getJournalpostId())
                && Objects.equals(v1.getPeriode().getPayload(), v2.getPeriode().getPayload()); // merk bruker payload her og ikke OppgittFraværPeriode, da den har equals på fom/tom
        };
        LocalDateSegmentCombinator<WrappedOppgittFraværPeriode, WrappedOppgittFraværPeriode, WrappedOppgittFraværPeriode> nySegment = (i, v1, v2) -> {
            if (Objects.equals(v1, v2)) {
                return v1;
            } else {
                return new LocalDateSegment<>(i, kloneVerdierNyPeriode(v1.getValue(), i.getFomDato(), i.getTomDato()));
            }
        };
        return timeline.compress(equalsChecker, nySegment);
    }

    private static WrappedOppgittFraværPeriode kloneVerdierNyPeriode(WrappedOppgittFraværPeriode wfp, LocalDate fom, LocalDate tom) {
        OppgittFraværPeriode fp = new OppgittFraværPeriode(fom, tom, wfp.getPeriode());
        return new WrappedOppgittFraværPeriode(fp, wfp.getInnsendingstidspunkt(), wfp.getKravDokumentType(), wfp.getSøknadsfristUtfall());
    }

    private Utfall utledUtfall(VurdertSøktPeriode<OppgittFraværPeriode> pa) {
        if (Duration.ZERO.equals(pa.getRaw().getFraværPerDag())) {
            return Utfall.OPPFYLT;
        }
        return pa.getUtfall();
    }
}
