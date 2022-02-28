package no.nav.k9.sak.ytelse.omsorgspenger.inntektsmelding;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
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

    private static final BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> KUN_FRAVÆR_PERIODE_MED_REFUSJONSKRAV_ELLER_TREKK_AV_DAGER = (dokumentType, vurdertPeriode) -> dokumentType == KravDokumentType.INNTEKTSMELDING || harTrektKrav(vurdertPeriode.getRaw().getFraværPerDag());
    private static final BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> KUN_FRAVÆR_PERIODE_UTEN_REFUSJONSKRAV_ELLER_TREKK_AV_DAGER = (dokumentType, vurdertPeriode) -> dokumentType == KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV || harTrektKrav(vurdertPeriode.getRaw().getFraværPerDag());
    private static final BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> IKKE_FILTRER = (dokumentType, vurdertPeriode) -> true;

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad = utledFraværsperioder(fraværFraKravdokumenter, SØKNAD_TYPER, IKKE_FILTRER);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderImMedRefusjonskrav = utledFraværsperioder(fraværFraKravdokumenter, IM_TYPER, KUN_FRAVÆR_PERIODE_MED_REFUSJONSKRAV_ELLER_TREKK_AV_DAGER);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderImUtenRefusjonskrav = utledFraværsperioder(fraværFraKravdokumenter, IM_TYPER, KUN_FRAVÆR_PERIODE_UTEN_REFUSJONSKRAV_ELLER_TREKK_AV_DAGER);

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> søknadOgImUtenRefusjonskravSammenslått = slåSammenSøknadOgImUtenRefusjonskrav(fraværsperioderSøknad, fraværsperioderImUtenRefusjonskrav);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> søknadOgAlleImSammenslått = slåSammenSøknadOgImMedRefusjonskrav(søknadOgImUtenRefusjonskravSammenslått, fraværsperioderImMedRefusjonskrav);

        return søknadOgAlleImSammenslått.values().stream()
            .map(KravDokumentFravær::compress)
            .flatMap(LocalDateTimeline::stream)
            .map(LocalDateSegment::getValue)
            .toList();
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> utledFraværsperioder(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter, Set<KravDokumentType> kravdokumentTyper, BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> periodefilter) {
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> mapByAktivitet = new LinkedHashMap<>();

        fraværFraKravdokumenter.entrySet().stream()
            .filter(e -> kravdokumentTyper.contains(e.getKey().getType()))
            .sorted(Comparator.comparing(e -> e.getKey().getInnsendingsTidspunkt()))
            .forEachOrdered(dokumentEntry -> {
                KravDokument dok = dokumentEntry.getKey();
                var fraværPerioder = dokumentEntry.getValue();
                var aktivitetIdent = unikAktivitetIdentifikator(fraværPerioder);

                var wrappedFaværPerioder = fraværPerioder.stream()
                    .filter(vurdertPeriode -> periodefilter.test(dok.getType(), vurdertPeriode))
                    .map(v -> new WrappedOppgittFraværPeriode(v.getRaw(), dok.getInnsendingsTidspunkt(), dok.getType(), utledUtfall(v), initiellKravtype(dok.getType(), v.getRaw().getFraværPerDag())))
                    .toList();

                var tidslinjeNy = mapTilTimeline(wrappedFaværPerioder);
                var tidslinjeSammenslått = mapByAktivitet.getOrDefault(aktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);

                ryddOppIBerørteArbeidsforhold(mapByAktivitet, aktivitetIdent, tidslinjeNy);
                mapByAktivitet.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinjeNy));
            });

        return mapByAktivitet;
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> slåSammenSøknadOgImMedRefusjonskrav(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderImMedRefusjonskrav) {

        // begynner med fraværsperioder fra søknad som seed
        var fraværsperioderSammenslått = new LinkedHashMap<>(fraværsperioderSøknad);

        // merge med fraværsperioder fra inntektsmelding
        fraværsperioderImMedRefusjonskrav.forEach((aktivitetIdent, tidslinjeIm) -> {
            var tidslinjeSøknad = finnSøknadTidslinje(fraværsperioderSøknad, aktivitetIdent);
            var tidslinjeSøknadOgRefusjonskrav = slåSammenSøknadOgRefusjonskrav(tidslinjeIm, tidslinjeSøknad);

            ryddOppIBerørteArbeidsforhold(fraværsperioderSammenslått, aktivitetIdent, tidslinjeIm);

            var tidslinjeRefusjon = tidslinjeSøknadOgRefusjonskrav.filterValue(v -> v.getKravDokumentType() != KravDokumentType.SØKNAD);
            oppdaterTidslinjeForAktivitet(fraværsperioderSammenslått, aktivitetIdent, tidslinjeRefusjon);

            //for de deler hvor IM er trekt, 'vinner' søknad. Søknad er registret på aktivitet uten arbeidsforhold satt, så må oppdateres der
            var tidslinjeRefusjonskravTrukketSøknadFinnes = tidslinjeSøknadOgRefusjonskrav.filterValue(v -> v.getKravDokumentType() == KravDokumentType.SØKNAD);
            if (!tidslinjeRefusjonskravTrukketSøknadFinnes.isEmpty()) {
                AktivitetIdentifikator søknadAktivitetIdent = finnSøknadAktivitetIdent(fraværsperioderSøknad.keySet(), aktivitetIdent).orElseThrow();
                oppdaterTidslinjeForAktivitet(fraværsperioderSammenslått, søknadAktivitetIdent, tidslinjeRefusjonskravTrukketSøknadFinnes);
            }
        });
        return fraværsperioderSammenslått;
    }

    void oppdaterTidslinjeForAktivitet(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått, AktivitetIdentifikator aktivitetIdent, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinje) {
        var tidslinjeSammenslått = fraværsperioderSammenslått.getOrDefault(aktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
        fraværsperioderSammenslått.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinje));
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> slåSammenSøknadOgImUtenRefusjonskrav(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderImUtenRefusjonskrav) {

        // registrerer på søknadene hvor det finnes samtidige fraværsperioder fra IM uten refusjonskrav
        var resultat = new LinkedHashMap<>(fraværsperioderSøknad);
        fraværsperioderImUtenRefusjonskrav.forEach((aktivitetIdent, tidslinjeIm) -> {
            finnSøknadAktivitetIdent(resultat.keySet(), aktivitetIdent).ifPresent(søknadAktivitetIdent -> {
                var tidslinjeSøknad = resultat.get(søknadAktivitetIdent);
                resultat.put(søknadAktivitetIdent, registrerStøtteFraInntektsmeldinger(tidslinjeSøknad, tidslinjeIm));
            });
        });
        return resultat;
    }

    private SamtidigKravStatus initiellKravtype(KravDokumentType type, Duration fraværPerDag) {
        return switch (type) {
            case SØKNAD -> harTrektKrav(fraværPerDag)
                ? SamtidigKravStatus.søknadTrekt()
                : SamtidigKravStatus.søknadFinnes();
            case INNTEKTSMELDING -> harTrektKrav(fraværPerDag)
                ? SamtidigKravStatus.refusjonskravTrekt()
                : SamtidigKravStatus.refusjonskravFinnes();
            case INNTEKTSMELDING_UTEN_REFUSJONSKRAV -> harTrektKrav(fraværPerDag)
                ? SamtidigKravStatus.inntektsmeldingUtenRefusjonskravTrekt()
                : SamtidigKravStatus.inntektsmeldingUtenRefusjonskravFinnes();
        };
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> slåSammenTidslinjer(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSammenslått, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNy) {
        return tidslinjeSammenslått.combine(tidslinjeNy, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    /**
     * Tre ting på en gang:
     * - kopierer inn Søknadsårsak, Fraværsårsak fra søknad
     * - oppdaterer SamtidigKravStatus
     * - erstatter med søknad hvis IM er trukket
     */
    private LocalDateTimeline<WrappedOppgittFraværPeriode> slåSammenSøknadOgRefusjonskrav(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeIm, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSøknad) {
        return tidslinjeIm.combine(tidslinjeSøknad, this::oppdaterPeriode, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> registrerStøtteFraInntektsmeldinger(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSøknad, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeStøttendeIM) {
        return tidslinjeSøknad.combine(tidslinjeStøttendeIM, this::mergeSøknadStøttendeIM, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> finnSøknadTidslinje(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad, AktivitetIdentifikator aktivitetIdent) {

        return finnSøknadAktivitetIdent(fraværsperioderSøknad.keySet(), aktivitetIdent)
            .map(fraværsperioderSøknad::get)
            .orElse((LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
    }

    private Optional<AktivitetIdentifikator> finnSøknadAktivitetIdent(Collection<AktivitetIdentifikator> aktivitetIdentifikatorer, AktivitetIdentifikator aktivitetIdentIm) {
        return aktivitetIdentifikatorer.stream()
            .filter(k -> k.gjelderSamme(aktivitetIdentIm))
            .findFirst();
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> oppdaterPeriode(LocalDateInterval di, LocalDateSegment<WrappedOppgittFraværPeriode> førsteVersjon, LocalDateSegment<WrappedOppgittFraværPeriode> sisteVersjon) {
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
            return nyPeriode(di, sisteVersjon);
        }
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergeImMedSøknad(LocalDateInterval di, WrappedOppgittFraværPeriode im, WrappedOppgittFraværPeriode søknad) {
        boolean søknadTrekt = harTrektKrav(søknad);
        boolean imTrekt = harTrektKrav(im);
        var gjeldende = imTrekt && !søknadTrekt ? søknad : im;

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
            gjeldende.getSøknadsfristUtfall(),
            nySamtidigKravStatus(søknad.getSamtidigeKrav(), imTrekt)
        );

        return new LocalDateSegment<>(di, wrapped);
    }

    private static SamtidigKravStatus nySamtidigKravStatus(SamtidigKravStatus status, boolean imErTrekt) {
        if (!imErTrekt) {
            return status.oppdaterRefusjonskravFinnes();
        }
        if (status.inntektsmeldingMedRefusjonskrav() == SamtidigKravStatus.KravStatus.FINNES) {
            //kan ikke sette TREKT, siden det finnes krav fra et annet arbeidsforhold
            return status;
        }
        return status.oppdaterRefusjonskravTrekt();
    }

    private LocalDateSegment<WrappedOppgittFraværPeriode> mergeSøknadStøttendeIM(LocalDateInterval di, LocalDateSegment<WrappedOppgittFraværPeriode> søknad, LocalDateSegment<WrappedOppgittFraværPeriode> støttendeIm) {
        if (støttendeIm == null) {
            return nyPeriode(di, søknad);
        }
        WrappedOppgittFraværPeriode wofpSøknad = søknad.getValue();
        OppgittFraværPeriode fp = new OppgittFraværPeriode(di.getFomDato(), di.getTomDato(), wofpSøknad.getPeriode());
        boolean kanOppdatereStatus = wofpSøknad.getSamtidigeKrav().inntektsmeldingUtenRefusjonskrav() != SamtidigKravStatus.KravStatus.FINNES;
        var nyStatus = harTrektKrav(støttendeIm.getValue()) ? SamtidigKravStatus.KravStatus.TREKT : SamtidigKravStatus.KravStatus.FINNES;
        SamtidigKravStatus samtidigeKrav = kanOppdatereStatus
            ? wofpSøknad.getSamtidigeKrav().oppdaterInntektsmeldingUtenRefusjonskravStatus(nyStatus)
            : wofpSøknad.getSamtidigeKrav(); //verdi aggregeres på arbeidsgiver. siden OK at søker er borte fra bare ett arbeidsforhold, kan vi ikke sette TREKT som aggregert status når finnes JA på ett arbeidsforhold
        WrappedOppgittFraværPeriode wofp = new WrappedOppgittFraværPeriode(fp, wofpSøknad.getInnsendingstidspunkt(), wofpSøknad.getKravDokumentType(), wofpSøknad.getSøknadsfristUtfall(), samtidigeKrav);
        return new LocalDateSegment<>(di, wofp);
    }

    private static boolean harTrektKrav(WrappedOppgittFraværPeriode im) {
        return harTrektKrav(im.getPeriode().getFraværPerDag());
    }

    private static boolean harTrektKrav(Duration fraværPrDag) {
        return fraværPrDag != null && fraværPrDag.isZero();
    }

    /**
     * Rydd opp i arbeidsforhold for samme arbeidsgiver, men annet arbeidsforhold
     */
    private void ryddOppIBerørteArbeidsforhold(Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> mapByAktivitet,
                                               AktivitetIdentifikator aktivitetIdent,
                                               LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeNy) {

        for (AktivitetIdentifikator key : List.copyOf(mapByAktivitet.keySet())) {
            if (!key.equals(aktivitetIdent) && key.gjelderSamme(aktivitetIdent)) {
                var tidslinjeBerørt = mapByAktivitet.get(key);
                tidslinjeBerørt = tidslinjeBerørt.disjoint(tidslinjeNy, KravDokumentFravær::leftOnly);
                mapByAktivitet.put(key, tidslinjeBerørt);
            }
        }
    }

    private LocalDateTimeline<WrappedOppgittFraværPeriode> mapTilTimeline(Collection<WrappedOppgittFraværPeriode> aktiviteter) {
        return new LocalDateTimeline<>(aktiviteter.stream()
            .map(a -> new LocalDateSegment<>(a.getPeriode().getFom(), a.getPeriode().getTom(), a))
            .toList());
    }

    private static LocalDateSegment<WrappedOppgittFraværPeriode> nyPeriode(LocalDateInterval nyPeriode, LocalDateSegment<WrappedOppgittFraværPeriode> gjeldende) {
        return new LocalDateSegment<>(nyPeriode, nyPeriode(gjeldende.getValue(), nyPeriode.getFomDato(), nyPeriode.getTomDato()));
    }

    public static LocalDateSegment<WrappedOppgittFraværPeriode> leftOnly(LocalDateInterval dateInterval, LocalDateSegment<WrappedOppgittFraværPeriode> lhs, LocalDateSegment<WrappedOppgittFraværPeriode> rhs) {
        return new LocalDateSegment<>(dateInterval, nyPeriode(lhs.getValue(), dateInterval.getFomDato(), dateInterval.getTomDato()));
    }

    public static LocalDateTimeline<WrappedOppgittFraværPeriode> compress(LocalDateTimeline<WrappedOppgittFraværPeriode> timeline) {
        return timeline.compress(
            WrappedOppgittFraværPeriode::equalsIgnorerPeriode,
            (intervall, v1, v2) -> new LocalDateSegment<>(intervall, nyPeriode(v1.getValue(), intervall.getFomDato(), intervall.getTomDato())));
    }

    private static WrappedOppgittFraværPeriode nyPeriode(WrappedOppgittFraværPeriode wfp, LocalDate fom, LocalDate tom) {
        OppgittFraværPeriode fp = new OppgittFraværPeriode(fom, tom, wfp.getPeriode());
        return new WrappedOppgittFraværPeriode(fp, wfp.getInnsendingstidspunkt(), wfp.getKravDokumentType(), wfp.getSøknadsfristUtfall(), wfp.getSamtidigeKrav());
    }

    private Utfall utledUtfall(VurdertSøktPeriode<OppgittFraværPeriode> pa) {
        if (Duration.ZERO.equals(pa.getRaw().getFraværPerDag())) {
            return Utfall.OPPFYLT;
        }
        return pa.getUtfall();
    }

    private AktivitetIdentifikator unikAktivitetIdentifikator(Collection<VurdertSøktPeriode<OppgittFraværPeriode>> søktePerioder) {
        Set<AktivitetIdentifikator> resultat = søktePerioder.stream().map(AktivitetIdentifikator::lagAktivitetIdentifikator).collect(Collectors.toSet());
        if (resultat.size() == 1) {
            return resultat.iterator().next();
        }
        throw new IllegalArgumentException("Forventer nøyaktig 1 aktivitet identifikator pr kravdokument, men fikk " + resultat.size());
    }
}
