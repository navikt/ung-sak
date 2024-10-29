package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
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
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
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
     * @param yrkesaktiviteter Yrkesaktiviteter
     * @return Liste med utbetalingsgrader og perioder pr aktivitet
     */
    static List<UtbetalingsgradPrAktivitetDto> finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet vilkårsperiode,
                                                                                       Uttaksplan uttaksplan,
                                                                                       List<Arbeid> arbeidIPeriode,
                                                                                       Collection<Yrkesaktivitet> yrkesaktiviteter) {
        Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet = finnUtbetalingsgradOgPeriodePrAktivitet(vilkårsperiode, uttaksplan);
        leggTilFrilansSomIgnoreresIUttak(arbeidIPeriode, yrkesaktiviteter, utbetalingsgradPrAktivitet);
        return mapTilKalkulusKontrakt(vilkårsperiode, utbetalingsgradPrAktivitet);
    }

    /** Legger til 0/0 frilansaktivitet som ikke er inkludert i uttaksplanen dersom bruker har aktiv frilansaktivitet
     * @param arbeidIPeriode Oppgitt arbeidsinformasjon fra søknad
     * @param yrkesaktiviteter Yrkesaktiviteter
     * @param utbetalingsgradPrAktivitet Utbetalingsgrader mappet fra uttaksplanen
     */
    private static void leggTilFrilansSomIgnoreresIUttak(List<Arbeid> arbeidIPeriode, Collection<Yrkesaktivitet> yrkesaktiviteter, Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet) {
        var frilansperioder = new ArrayList<>(finnPerioderMappetFraUttak(utbetalingsgradPrAktivitet));
        var frilansAktivitetFraSøknadSomIkkeErMedIUttaket = finnFrilansaktivitetSomSkalLeggesTil(yrkesaktiviteter, arbeidIPeriode);
        frilansAktivitetFraSøknadSomIkkeErMedIUttaket.forEach(p -> {
            if (frilansperioder.stream().noneMatch(eksisterendePeriode -> eksisterendePeriode.periode().overlapper(p))) {
                frilansperioder.add(new PeriodeMedGrad(p, BigDecimal.ZERO, BigDecimal.valueOf(100)));
            }
        });
        if (!frilansperioder.isEmpty()) {
            utbetalingsgradPrAktivitet.put(new AktivitetDto(null, null, UttakArbeidType.FRILANS), frilansperioder);
        }
    }

    private static List<PeriodeMedGrad> finnPerioderMappetFraUttak(Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet.getOrDefault(new AktivitetDto(null, null, UttakArbeidType.FRILANS), Collections.emptyList());
    }

    private static List<UtbetalingsgradPrAktivitetDto> mapTilKalkulusKontrakt(DatoIntervallEntitet vilkårsperiode,
                                                                              Map<AktivitetDto, List<PeriodeMedGrad>> utbetalingsgradPrAktivitet) {
        return utbetalingsgradPrAktivitet.entrySet().stream().map(entry -> {
                var tidslinje = lagTidslinje(entry.getValue());
                var mappet = tidslinje.intersection(vilkårsperiode.toLocalDateInterval())
                    .stream()
                    .map(s -> new PeriodeMedUtbetalingsgradDto(
                        new Periode(s.getFom(), s.getTom()),
                        Utbetalingsgrad.fra(s.getValue().utbetalingsgrad()),
                        Aktivitetsgrad.fra(s.getValue().aktivitetsgrad()))).toList();
                return new UtbetalingsgradPrAktivitetDto(entry.getKey(), mappet);
            }
        ).toList();
    }


    private static Map<AktivitetDto, List<PeriodeMedGrad>> finnUtbetalingsgradOgPeriodePrAktivitet(DatoIntervallEntitet gyldigePerioder, Uttaksplan uttaksplan) {
        return uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(it -> gyldigePerioder.overlapper(tilDatoIntervall(it.getKey())))
            .map(MapTilUtbetalingsgradPrAktivitet::lagAktivitetTilPeriodeMap)
            .reduce(mergeMaps())
            .orElse(Collections.emptyMap());
    }

    private static Map<AktivitetDto, List<PeriodeMedGrad>> lagAktivitetTilPeriodeMap(Map.Entry<LukketPeriode, UttaksperiodeInfo> e) {
        return e.getValue().getUtbetalingsgrader()
            .stream()
            .collect(
                Collectors.toMap(
                    // Keymapper: Aktivitet
                    MapTilUtbetalingsgradPrAktivitet::mapUtbetalingsgradArbeidsforhold,
                    // Valuemapper: Liste av perioder med utbetalingsgrad (for å gjøre det enklere å gjøre reduce etterpå)
                    a -> List.of(tilPeriodeMedGrad(e.getKey(), a.getUtbetalingsgrad(), FinnAktivitetsgrad.finnAktivitetsgrad(a)))));
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

    private static PeriodeMedGrad tilPeriodeMedGrad(LukketPeriode e, BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
        return new PeriodeMedGrad(tilDatoIntervall(e), utbetalingsgrad, aktivitetsgrad);
    }

    private static DatoIntervallEntitet tilDatoIntervall(LukketPeriode e) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(e.getFom(), e.getTom());
    }


    /**
     * Siden søknadsdialogen sender Frilans 0/0 i alle søknader (også dei uten frilans i aareg),
     * må vi finne ut om vi skal sende med frilans med 0/0 basert på om dette er registrert i aareg.
     * Dette må vi gjøre fordi 0/0 brukes til å angi at en aktivitet ikke skal tas med i uttak og vil dermed ikke gi en utbetalingsgrad herfra.
     *
     * @param alleYrkesaktiviteter Alle yrkesaktiviteter
     * @param arbeidIPeriode       Arbeid fra søknad
     * @return
     */
    private static NavigableSet<DatoIntervallEntitet> finnFrilansaktivitetSomSkalLeggesTil(Collection<Yrkesaktivitet> alleYrkesaktiviteter, List<Arbeid> arbeidIPeriode) {
        return arbeidIPeriode.stream()
            .filter(MapTilUtbetalingsgradPrAktivitet::gjelderFrilans)
            .flatMap(a -> {
                var ansettelseFrilansTidslinje = finnAnsettelsestidslinje(alleYrkesaktiviteter);
                var nullOverNullTidslinje = finnTidslinjeNullOverNull(a);
                var tidslinjeAvInteresse = ansettelseFrilansTidslinje.intersection(nullOverNullTidslinje);
                return TidslinjeUtil.tilDatoIntervallEntiteter(tidslinjeAvInteresse).stream();
            }).collect(Collectors.toCollection(TreeSet::new));
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeNullOverNull(Arbeid a) {
        var nullOverNullPerioder = finnNullOverNullPerioder(a);
        return TidslinjeUtil.tilTidslinjeKomprimertMedMuligOverlapp(nullOverNullPerioder);
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


    private static LocalDateTimeline<AktivitetsgradOgUtbetalingsgrad> lagTidslinje(List<PeriodeMedGrad> perioder) {
        return perioder.stream()
            .map(p -> new LocalDateTimeline<>(p.periode.getFomDato(), p.periode.getTomDato(), new AktivitetsgradOgUtbetalingsgrad(p.utbetalingsgrad(), p.aktivitetsgrad())))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
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

    private record PeriodeMedGrad(DatoIntervallEntitet periode, BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
    }

    private record AktivitetsgradOgUtbetalingsgrad(BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
    }
}
