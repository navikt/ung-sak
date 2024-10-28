package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktivitetsgrad;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Utbetalingsgrad;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.domene.typer.tid.Hjelpetidslinjer;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

class MapTilUtbetalingsgradPrAktivitet {


    /**
     * Finner utbetalingsgrader fra uttak og aktivitetsgrader fra søknad og mapper informasjonen til kalkulus-kontrakt.
     * Grunnen til at vi må ta hensyn til aktiviteter som ligger i både uttak og søknad er fordi søknaden kan ha informasjon om aktiviteter som ikke ligger i uttak (f.eks omsorgsstønad)
     *
     * @param vilkårsperiode   Vilkårsperiode det beregnes for
     * @param uttaksplan       Uttaksplan
     * @param arbeidIPeriode   Arbeid oppgitt i søknad
     * @param yrkesaktiviteter
     * @return Liste med utbetalingsgrader og perioder pr aktivitet
     */
    static List<UtbetalingsgradPrAktivitetDto> finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet vilkårsperiode,
                                                                                       Optional<Uttaksplan> uttaksplan,
                                                                                       List<Arbeid> arbeidIPeriode,
                                                                                       Collection<Yrkesaktivitet> yrkesaktiviteter) {
        Set<AktivitetDto> aktiviteter = finnAlleAktiviteterIPeriode(vilkårsperiode, uttaksplan, arbeidIPeriode, yrkesaktiviteter);
        Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet = finnUtbetalingsgradOgPeriodePrAktivitet(vilkårsperiode, uttaksplan);
        Map<AktivitetDto, List<PeriodeMedGrad>> aktivitetsgradPrAktivitet = finnAktivitetsgradOgPeriodePrAktivitet(arbeidIPeriode, yrkesaktiviteter);
        return kombinerOgMapTilKalkulusKontrakt(vilkårsperiode, aktiviteter, utbetalingsgradPrAktivitet, aktivitetsgradPrAktivitet);
    }

    private static List<UtbetalingsgradPrAktivitetDto> kombinerOgMapTilKalkulusKontrakt(DatoIntervallEntitet vilkårsperiode,
                                                                                        Set<AktivitetDto> aktiviteter,
                                                                                        Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet,
                                                                                        Map<AktivitetDto, List<PeriodeMedGrad>> aktivitetsgradPrAktivitet) {
        return aktiviteter.stream().map(a -> {
                var sammenslått = mapTilSammenslåttTidslinje(a, utbetalingsgradPrAktivitet, aktivitetsgradPrAktivitet);
                var mappet = Hjelpetidslinjer.fjernHelger(sammenslått)
                    .intersection(vilkårsperiode.toLocalDateInterval())
                    .stream()
                    .map(s -> new PeriodeMedUtbetalingsgradDto(
                        new Periode(s.getFom(), s.getTom()),
                        Utbetalingsgrad.fra(s.getValue().utbetalingsgrad()),
                        Aktivitetsgrad.fra(s.getValue().aktivitetsgrad()))).toList();
                return new UtbetalingsgradPrAktivitetDto(a, mappet);
            }
        ).toList();
    }

    private static LocalDateTimeline<Boolean> finnUttakTidslinje(Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet.values().stream()
            .flatMap(Collection::stream)
            .map(PeriodeMedGrad::periode)
            .map(p -> new LocalDateTimeline<>(p.getFomDato(), p.getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static Map<AktivitetDto, List<PeriodeMedGrad>> finnUtbetalingsgradOgPeriodePrAktivitet(DatoIntervallEntitet gyldigePerioder, Optional<Uttaksplan> uttaksplan) {
        return uttaksplan.flatMap(plan -> plan.getPerioder()
                .entrySet()
                .stream()
                .filter(it -> gyldigePerioder.overlapper(tilDatoIntervall(it.getKey())))
                .map(MapTilUtbetalingsgradPrAktivitet::lagAktivitetTilPeriodeMap)
                .reduce(mergeMaps()))
            .orElse(Collections.emptyMap());
    }

    private static Map<AktivitetDto, List<PeriodeMedGrad>> lagAktivitetTilPeriodeMap(Map.Entry<LukketPeriode, UttaksperiodeInfo> e) {
        return e.getValue().getUtbetalingsgrader()
            .stream()
            .collect(
                Collectors.toMap(
                    // Keymapper: Aktivitet
                    MapTilUtbetalingsgradPrAktivitet::mapUtbetalingsgradArbeidsforhold,
                    // Valuemapper: Liste av perioder med grad (for å gjøre det enklere å gjøre reduce etterpå)
                    a -> List.of(tilPeriodeMedGrad(e.getKey(), a.getUtbetalingsgrad()))));
    }


    /**
     * Slår sammen to maps av perioder pr aktivite ved å ta alle entries fra den ene og legge inn i den andre. Ved konflikt slår vi sammen periodelistene
     *
     * @return Funksjon for å slå sammen to maps
     */
    private static BinaryOperator<Map<AktivitetDto, List<PeriodeMedGrad>>> mergeMaps() {
        // Slår sammen verdier fra to maps. Ingen av periodene er overlappende siden de gjelder for samme aktivitet og kommer fra samme uttaksplan
        return (m1, m2) -> {
            m1.forEach((k, v) -> m2.merge(k, v, kombinerLister()));
            return m2;
        };
    }

    /**
     * Slår sammen to lister av perioder der periodene antas å være ikke-overlappende
     *
     * @return Funksjon for å kombinere to lister av perioder
     */
    private static BiFunction<List<PeriodeMedGrad>, List<PeriodeMedGrad>, List<PeriodeMedGrad>> kombinerLister() {
        return (perioderMedGrad1, perioderMedGrad2) -> {
            var nyListe = new ArrayList<>(perioderMedGrad1);
            nyListe.addAll(perioderMedGrad2);
            return nyListe;
        };
    }

    private static PeriodeMedGrad tilPeriodeMedGrad(LukketPeriode e, BigDecimal a) {
        return new PeriodeMedGrad(tilDatoIntervall(e), a);
    }

    private static DatoIntervallEntitet tilDatoIntervall(LukketPeriode e) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(e.getFom(), e.getTom());
    }

    private static Map<AktivitetDto, List<PeriodeMedGrad>> finnAktivitetsgradOgPeriodePrAktivitet(List<Arbeid> arbeidIPeriode, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        return arbeidIPeriode.stream()
            .filter(a -> !a.getPerioder().isEmpty())
            .collect(Collectors.toMap(
                MapTilUtbetalingsgradPrAktivitet::tilAktivitetDto,
                a1 -> finnPerioderMedAktivitetsgrad(a1, yrkesaktiviteter)
            ));
    }

    /**
     * Siden søknadsdialogen sender Frilans 0/0 i alle søknader (også dei uten frilans i aareg),
     * må vi finne ut om vi skal sende med frilans med 0/0 basert på om dette er registrert i aareg.
     * Dette må vi gjøre fordi 0/0 brukes til å angi at en aktivitet ikke skal tas med i uttak og vil dermed ikke gi en utbetalingsgrad herfra.
     *
     * @param alleYrkesaktiviteter Alle yrkesaktiviteter
     * @param a                    Arbeid fra søknad
     * @return
     */
    private static NavigableSet<DatoIntervallEntitet> finnRelevantePerioderForAktivitet(Collection<Yrkesaktivitet> alleYrkesaktiviteter, Arbeid a) {
        if (gjelderFrilans(a)) {
            var ansettelseFrilansTidslinje = finnAnsettelsestidslinje(alleYrkesaktiviteter);
            var tidslinjeSøktIkkeNullOverNull = finnTidslinjeIkkeNullOverNull(a);
            var nullOverNullTidslinje = finnTidslinjeNullOverNull(a);
            var tidslinjeAvInteresse = ansettelseFrilansTidslinje.intersection(nullOverNullTidslinje).crossJoin(tidslinjeSøktIkkeNullOverNull);
            return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeAvInteresse);
        }

        return finnSøktePerioder(a);
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeNullOverNull(Arbeid a) {
        var nullOverNullPerioder = finnNullOverNullPerioder(a);
        return TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(nullOverNullPerioder);
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeIkkeNullOverNull(Arbeid a) {
        var periodeUtenNullOverNull = a.getPerioder().entrySet().stream()
            .filter(v -> !erNullOverNull(v))
            .map(Map.Entry::getKey)
            .map(MapTilUtbetalingsgradPrAktivitet::tilDatoIntervall)
            .toList();
        return TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(periodeUtenNullOverNull);
    }

    private static LocalDateTimeline<Boolean> finnAnsettelsestidslinje(Collection<Yrkesaktivitet> alleYrkesaktiviteter) {
        var ansettelsesperioderFrilans = alleYrkesaktiviteter.stream().filter(ya -> ya.getArbeidType().equals(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER))
            .flatMap(ya -> ya.getAnsettelsesPeriode().stream())
            .map(AktivitetsAvtale::getPeriode)
            .toList();
        return TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(ansettelsesperioderFrilans);
    }

    private static List<DatoIntervallEntitet> finnNullOverNullPerioder(Arbeid a) {
        return a.getPerioder().entrySet().stream().filter(MapTilUtbetalingsgradPrAktivitet::erNullOverNull)
            .map(Map.Entry::getKey)
            .map(MapTilUtbetalingsgradPrAktivitet::tilDatoIntervall)
            .toList();
    }

    private static boolean erNullOverNull(Map.Entry<LukketPeriode, ArbeidsforholdPeriodeInfo> v) {
        return v.getValue().getJobberNå().isZero() && v.getValue().getJobberNormalt().isZero();
    }

    private static boolean gjelderFrilans(Arbeid a) {
        return no.nav.k9.kodeverk.uttak.UttakArbeidType.FRILANSER.getKode().equals(a.getArbeidsforhold().getType());
    }

    private static List<PeriodeMedGrad> finnPerioderMedAktivitetsgrad(Arbeid a, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var relevantePerioder = finnRelevantePerioderForAktivitet(yrkesaktiviteter, a);
        var relevantPeriodeTidslinje = TidslinjeUtil.tilTidslinjeKomprimert(relevantePerioder);
        var oppgittGradTidslinje = a.getPerioder()
            .entrySet()
            .stream()
            .map(p -> new LocalDateTimeline<>(p.getKey().getFom(), p.getKey().getTom(), FinnAktivitetsgrad.finnAktivitetsgrad(p.getValue())))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
        return oppgittGradTidslinje.intersection(relevantPeriodeTidslinje)
            .toSegments()
            .stream()
            .map(s -> new PeriodeMedGrad(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()), s.getValue()))
            .toList();
    }

    private static LocalDateTimeline<AktivitetsgradOgUtbetalingsgrad> mapTilSammenslåttTidslinje(AktivitetDto a,
                                                                                                 Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet,
                                                                                                 Map<AktivitetDto, List<PeriodeMedGrad>> aktivitetsgradPrAktivitet) {
        var utbetalingsgradTidslinje = lagTidslinje(a, utbetalingsgradPrAktivitet);
        var aktivitetsgradTidslinje = lagTidslinje(a, aktivitetsgradPrAktivitet);
        // Tar med alle perioder fra begge tidslinjer
        return utbetalingsgradTidslinje.crossJoin(aktivitetsgradTidslinje, MapTilUtbetalingsgradPrAktivitet::kombinerInformasjon);
    }

    private static LocalDateSegment<AktivitetsgradOgUtbetalingsgrad> kombinerInformasjon(LocalDateInterval di,
                                                                                         LocalDateSegment<BigDecimal> utbetalingsgradSegment,
                                                                                         LocalDateSegment<BigDecimal> aktivitetsgradSegment) {
        if (utbetalingsgradSegment == null) {
            // Dersom vi ikke har utbetalingsgrad settes denne til 0 (det skal aldri utbetales med mindre dette kommer fra uttak)
            return new LocalDateSegment<>(di, new AktivitetsgradOgUtbetalingsgrad(BigDecimal.ZERO, aktivitetsgradSegment.getValue()));
        } else if (aktivitetsgradSegment == null) {
            // Dersom vi ikke har aktivitetgrad antar vi at bruker er i aktivitet i den delen det ikke utbetales pleiepenger for
            return new LocalDateSegment<>(di, new AktivitetsgradOgUtbetalingsgrad(utbetalingsgradSegment.getValue(), BigDecimal.valueOf(100).subtract(utbetalingsgradSegment.getValue())));
        }
        return new LocalDateSegment<>(di, new AktivitetsgradOgUtbetalingsgrad(utbetalingsgradSegment.getValue(), aktivitetsgradSegment.getValue()));
    }

    private static LocalDateTimeline<BigDecimal> lagTidslinje(AktivitetDto a, Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet.getOrDefault(a, Collections.emptyList()).stream()
            .map(p -> new LocalDateTimeline<>(p.periode.getFomDato(), p.periode.getTomDato(), p.grad))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static Set<AktivitetDto> finnAlleAktiviteterIPeriode(DatoIntervallEntitet gyldigePerioder, Optional<Uttaksplan> uttaksplan, List<Arbeid> arbeidIPeriode, Collection<Yrkesaktivitet> yrkesaktiviteter) {
        var aktiviteter = uttaksplan.map(plan -> plan.getPerioder()
            .entrySet()
            .stream()
            .filter(it -> gyldigePerioder.overlapper(tilDatoIntervall(it.getKey())))
            .map(Map.Entry::getValue)
            .flatMap(u -> u.getUtbetalingsgrader().stream())
            .map(MapTilUtbetalingsgradPrAktivitet::mapUtbetalingsgradArbeidsforhold)
            .collect(Collectors.toCollection(HashSet::new))).orElse(new HashSet<>());

        var aktivitetFraRapportertArbeid = arbeidIPeriode.stream()
            .filter(a -> !a.getPerioder().isEmpty())
            .filter(a -> {
                var relevantePerioder = finnRelevantePerioderForAktivitet(yrkesaktiviteter, a);
                var søktePerioder = finnSøktePerioder(a);
                return harOverlapp(søktePerioder, relevantePerioder);
            })
            .filter(a -> a.getPerioder().keySet().stream().anyMatch(it -> gyldigePerioder.overlapper(tilDatoIntervall(it))))
            .map(MapTilUtbetalingsgradPrAktivitet::tilAktivitetDto).collect(Collectors.toCollection(HashSet::new));

        aktiviteter.addAll(aktivitetFraRapportertArbeid);
        return aktiviteter;
    }

    private static TreeSet<DatoIntervallEntitet> finnSøktePerioder(Arbeid a) {
        return a.getPerioder().keySet().stream()
            .map(MapTilUtbetalingsgradPrAktivitet::tilDatoIntervall)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean harOverlapp(NavigableSet<DatoIntervallEntitet> søktePerioder, NavigableSet<DatoIntervallEntitet> relevantePerioder) {
        return søktePerioder.stream()
            .anyMatch(p1 -> relevantePerioder.stream().anyMatch(p1::overlapper));
    }

    private static AktivitetDto tilAktivitetDto(Arbeid a) {
        Aktør aktør = null;
        if (a.getArbeidsforhold().getOrganisasjonsnummer() != null) {
            aktør = new Organisasjon(a.getArbeidsforhold().getOrganisasjonsnummer());
        } else if (a.getArbeidsforhold().getAktørId() != null) {
            aktør = new AktørIdPersonident(a.getArbeidsforhold().getAktørId());
        }
        return new AktivitetDto(aktør, a.getArbeidsforhold().getArbeidsforholdId() == null ? null : new InternArbeidsforholdRefDto(a.getArbeidsforhold().getArbeidsforholdId()),
            mapUttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType.fraKode(a.getArbeidsforhold().getType())));
    }

    private static AktivitetDto mapUtbetalingsgradArbeidsforhold(Utbetalingsgrader utbGrad) {
        Arbeidsforhold arbeidsforhold = utbGrad.getArbeidsforhold();
        if (erTypeMedArbeidsforhold(arbeidsforhold)) {
            return lagArbeidsforhold(arbeidsforhold);
        } else {
            return new AktivitetDto(null, null, mapUttakArbeidType(arbeidsforhold));
        }
    }

    private static boolean erTypeMedArbeidsforhold(Arbeidsforhold arbeidsforhold) {
        return arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.ARBEIDSTAKER.getKode()) ||
            arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV.getKode()) ||
            arbeidsforhold.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING.getKode());
    }

    private static AktivitetDto lagArbeidsforhold(Arbeidsforhold arb) {
        return new AktivitetDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            mapUttakArbeidType(arb));
    }

    private static UttakArbeidType mapUttakArbeidType(Arbeidsforhold arb) {
        return mapUttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType.fraKode(arb.getType()));
    }

    private static UttakArbeidType mapUttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ARBEIDSTAKER -> UttakArbeidType.ORDINÆRT_ARBEID;
            case FRILANSER -> UttakArbeidType.FRILANS;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case DAGPENGER -> UttakArbeidType.DAGPENGER;
            case INAKTIV -> UttakArbeidType.MIDL_INAKTIV;
            case KUN_YTELSE -> UttakArbeidType.BRUKERS_ANDEL;
            case IKKE_YRKESAKTIV, IKKE_YRKESAKTIV_UTEN_ERSTATNING -> UttakArbeidType.IKKE_YRKESAKTIV;
            case PLEIEPENGER_AV_DAGPENGER -> UttakArbeidType.PLEIEPENGER_AV_DAGPENGER;
            case SYKEPENGER_AV_DAGPENGER -> UttakArbeidType.SYKEPENGER_AV_DAGPENGER;
            case ANNET -> UttakArbeidType.ANNET;
            case null -> null;
        };
    }

    private static Aktør lagAktør(Arbeidsforhold arb) {
        if (arb.getAktørId() != null) {
            return new AktørIdPersonident(arb.getAktørId());
        } else if (arb.getOrganisasjonsnummer() != null) {
            return new Organisasjon(arb.getOrganisasjonsnummer());
        } else {
            return null;
        }
    }

    private record PeriodeMedGrad(DatoIntervallEntitet periode, BigDecimal grad) {
    }

    private record AktivitetsgradOgUtbetalingsgrad(BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
    }
}
