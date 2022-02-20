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

    public List<WrappedOppgittFraværPeriode> trekkUtAlleFraværOgValiderOverlapp(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad = utledKravFraSøknad(fraværFraKravdokumenter);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderRefusjonskrav = utledRefusjonskrav(fraværFraKravdokumenter);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> støttendeIM = utledStøttendeIM(fraværFraKravdokumenter);

        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknadStøtteIM = slåSammenSøknaderOgStøttendeIm(fraværsperioderSøknad, støttendeIM);
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSammenslått = slåSammenSøknadOgInntektsmelding(fraværsperioderSøknadStøtteIM, fraværsperioderRefusjonskrav);

        return fraværsperioderSammenslått.values().stream()
            .map(KravDokumentFravær::compress)
            .flatMap(LocalDateTimeline::stream)
            .map(LocalDateSegment::getValue)
            .toList();
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> utledRefusjonskrav(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        return utledFraværsperioder(fraværFraKravdokumenter, IM_TYPER, ignorerFraværPerioderUtenRefusjon);
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> utledKravFraSøknad(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        return utledFraværsperioder(fraværFraKravdokumenter, SØKNAD_TYPER, ikkeFiltrer);
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> utledStøttendeIM(Map<KravDokument, List<VurdertSøktPeriode<OppgittFraværPeriode>>> fraværFraKravdokumenter) {
        return utledFraværsperioder(fraværFraKravdokumenter, Set.of(KravDokumentType.INNTEKTSMELDING_UTEN_REFUSJONSKRAV), ikkeFiltrer);
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

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> slåSammenSøknadOgInntektsmelding(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderIm) {

        // begynner med fraværsperioder fra søknad som seed
        var fraværsperioderSammenslått = new LinkedHashMap<>(fraværsperioderSøknad);

        // merge med fraværsperioder fra inntektsmelding
        fraværsperioderIm.forEach((aktivitetIdent, tidslinjeIm) -> {
            var tidslinjeSøknad = finnSøknadTidslinje(fraværsperioderSøknad, aktivitetIdent);
            var tidlinjeImBeriket = berikMedSøknad(tidslinjeIm, tidslinjeSøknad);

            ryddOppIBerørteArbeidsforhold(fraværsperioderSammenslått, aktivitetIdent, tidslinjeIm);

            var tidslinjeSammenslått = fraværsperioderSammenslått.getOrDefault(aktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);

            var tidslinjeRefusjon = tidlinjeImBeriket.filterValue(v -> v.getKravDokumentType() != KravDokumentType.SØKNAD);
            fraværsperioderSammenslått.put(aktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinjeRefusjon));

            //for de deler hvor IM er trekt, 'vinner' søknad. Søknad er registret på aktivitet uten arbeidsforhold satt, så må oppdateres der
            var tidslinjeRefusjonskravTrukketSøknadFinnes = tidlinjeImBeriket.filterValue(v -> v.getKravDokumentType() == KravDokumentType.SØKNAD);
            if (!tidslinjeRefusjonskravTrukketSøknadFinnes.isEmpty()) {
                AktivitetIdentifikator søknadAktivitetIdent = finnSøknadAktivitetIdent(fraværsperioderSøknad.keySet(), aktivitetIdent).orElseThrow();
                tidslinjeSammenslått = fraværsperioderSammenslått.getOrDefault(søknadAktivitetIdent, (LocalDateTimeline<WrappedOppgittFraværPeriode>) LocalDateTimeline.EMPTY_TIMELINE);
                fraværsperioderSammenslått.put(søknadAktivitetIdent, slåSammenTidslinjer(tidslinjeSammenslått, tidslinjeRefusjonskravTrukketSøknadFinnes));
            }
        });
        return fraværsperioderSammenslått;
    }

    private Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> slåSammenSøknaderOgStøttendeIm(
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> fraværsperioderSøknad,
        Map<AktivitetIdentifikator, LocalDateTimeline<WrappedOppgittFraværPeriode>> støttendeIM) {

        // registrerer på søknadene hvor det finnes samtidige fraværsperioder fra IM uten refusjonskrav, her kalt støttende IM
        var fraværsperioderSøknadMedStøttendeIM = new LinkedHashMap<>(fraværsperioderSøknad);
        støttendeIM.forEach((aktivitetIdent, tidslinjeIm) -> {
            var tidslinjeSøknad = finnSøknadTidslinje(fraværsperioderSøknadMedStøttendeIM, aktivitetIdent);
            fraværsperioderSøknadMedStøttendeIM.put(aktivitetIdent, registrerStøtteFraInntektsmeldinger(tidslinjeSøknad, tidslinjeIm));
        });
        return fraværsperioderSøknadMedStøttendeIM;
    }

    private static BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> ignorerFraværPerioderUtenRefusjon = (dokumentType, vurdertPeriode) -> !erImUtenRefusjonskravOgUtenTrektPeriode(dokumentType, vurdertPeriode);

    private static BiPredicate<KravDokumentType, VurdertSøktPeriode<OppgittFraværPeriode>> ikkeFiltrer = (dokumentType, vurdertPeriode) -> true;

    private SamtidigKravStatus initiellKravtype(KravDokumentType type, Duration fraværPerDag) {
        return switch (type) {
            case SØKNAD -> SamtidigKravStatus.søknadFinnes();
            case INNTEKTSMELDING -> fraværPerDag == null || !fraværPerDag.isZero()
                ? SamtidigKravStatus.refusjonskravFinnes()
                : SamtidigKravStatus.refusjonskravTrekt();
            case INNTEKTSMELDING_UTEN_REFUSJONSKRAV -> SamtidigKravStatus.støttendeInntektsmeldingFinnes();
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
    private LocalDateTimeline<WrappedOppgittFraværPeriode> berikMedSøknad(LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeIm, LocalDateTimeline<WrappedOppgittFraværPeriode> tidslinjeSøknad) {
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

    private static boolean erImUtenRefusjonskravOgUtenTrektPeriode(KravDokumentType type, VurdertSøktPeriode<OppgittFraværPeriode> vurdertPeriode) {
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
        if (status.refusjonskrav() == SamtidigKravStatus.KravStatus.FINNES) {
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
        boolean kanOppdatereStatus = wofpSøknad.getSamtidigeKrav().støttendeInntektsmelding() != SamtidigKravStatus.KravStatus.FINNES;
        var nyStatus = harTrektKrav(støttendeIm.getValue()) ? SamtidigKravStatus.KravStatus.TREKT : SamtidigKravStatus.KravStatus.FINNES;
        SamtidigKravStatus samtidigeKrav = kanOppdatereStatus
            ? wofpSøknad.getSamtidigeKrav().oppdaterStøttendeImStatus(nyStatus)
            : wofpSøknad.getSamtidigeKrav(); //verdi aggregeres på arbeidsgiver. siden OK at søker er borte fra bare ett arbeidsforhold, kan vi ikke sette TREKT som aggregert status når finnes JA på ett arbeidsforhold
        WrappedOppgittFraværPeriode wofp = new WrappedOppgittFraværPeriode(fp, wofpSøknad.getInnsendingstidspunkt(), wofpSøknad.getKravDokumentType(), wofpSøknad.getSøknadsfristUtfall(), samtidigeKrav);
        return new LocalDateSegment<>(di, wofp);
    }

    private boolean harTrektKrav(WrappedOppgittFraværPeriode im) {
        return im.getPeriode().getFraværPerDag() != null && im.getPeriode().getFraværPerDag().isZero();
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
