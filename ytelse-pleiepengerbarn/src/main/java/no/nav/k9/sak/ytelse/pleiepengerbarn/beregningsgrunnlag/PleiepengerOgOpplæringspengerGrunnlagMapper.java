package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.OpplæringspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerNærståendeGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PleiepengerSyktBarnGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist.PSBVurdererSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.PeriodeFraSøknadForBrukerTjeneste;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.PerioderFraSøknad;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.ArbeidstidMappingInput;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.input.arbeid.MapArbeid;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class PleiepengerOgOpplæringspengerGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<YtelsespesifiktGrunnlagDto> {

    private UttakTjeneste uttakRestKlient;
    private UttakNyeReglerRepository uttakNyeReglerRepository;

    private PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste;

    private PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste;

    public PleiepengerOgOpplæringspengerGrunnlagMapper() {
        // for proxy
    }

    @Inject
    public PleiepengerOgOpplæringspengerGrunnlagMapper(UttakTjeneste uttakRestKlient, UttakNyeReglerRepository uttakNyeReglerRepository, PeriodeFraSøknadForBrukerTjeneste periodeFraSøknadForBrukerTjeneste, @Any PSBVurdererSøknadsfristTjeneste søknadsfristTjeneste) {
        this.uttakRestKlient = uttakRestKlient;
        this.uttakNyeReglerRepository = uttakNyeReglerRepository;
        this.periodeFraSøknadForBrukerTjeneste = periodeFraSøknadForBrukerTjeneste;
        this.søknadsfristTjeneste = søknadsfristTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var uttaksplan = uttakRestKlient.hentUttaksplan(ref.getBehandlingUuid(), false);


        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader = new ArrayList<>();
        if (uttaksplan != null) {
            utbetalingsgrader = finnUtbetalingsgrader(List.of(vilkårsperiode), uttaksplan);
        } else {
            var perioderFraSøknadene = periodeFraSøknadForBrukerTjeneste.hentPerioderFraSøknad(ref);
            var kravDokumenter = søknadsfristTjeneste.vurderSøknadsfrist(ref).keySet();
            utbetalingsgrader = finnUtbetalingsgraderFraSøknadsdata(vilkårsperiode, kravDokumenter, perioderFraSøknadene);
        }

        var datoForNyeRegler = uttakNyeReglerRepository.finnDatoForNyeRegler(ref.getBehandlingId());


        return mapTilYtelseSpesifikkType(ref, utbetalingsgrader, datoForNyeRegler);
    }

    private static List<UtbetalingsgradPrAktivitetDto> finnUtbetalingsgraderFraSøknadsdata(DatoIntervallEntitet vilkårsperiode, Set<KravDokument> kravDokumenter, Set<PerioderFraSøknad> perioderFraSøknadene) {
        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader;
        var timeline = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(vilkårsperiode.toLocalDateInterval(), true)));

        var arbeidstidInput = new ArbeidstidMappingInput(kravDokumenter,
            perioderFraSøknadene,
            timeline,
            null,
            null);
        var arbeidIPeriode = new MapArbeid().map(arbeidstidInput);
        utbetalingsgrader = arbeidIPeriode.stream().map(a -> {
            var perioder = a.getPerioder().entrySet().stream().map(p -> new PeriodeMedUtbetalingsgradDto(new Periode(p.getKey().getFom(), p.getKey().getTom()), BigDecimal.valueOf(100), hentAktivitetsgrad(p.getValue()))).toList();
            Aktør aktør = null;
            if (a.getArbeidsforhold().getOrganisasjonsnummer() != null) {
                aktør = new Organisasjon(a.getArbeidsforhold().getOrganisasjonsnummer());
            } else if (a.getArbeidsforhold().getAktørId() != null) {
                aktør = new AktørIdPersonident(a.getArbeidsforhold().getAktørId());
            }
            return new UtbetalingsgradPrAktivitetDto(new AktivitetDto(aktør, a.getArbeidsforhold().getArbeidsforholdId() == null ? null : new InternArbeidsforholdRefDto(a.getArbeidsforhold().getArbeidsforholdId()), new UttakArbeidType(a.getArbeidsforhold().getType())), perioder);
        }).toList();
        return utbetalingsgrader;
    }

    public static List<UtbetalingsgradPrAktivitetDto> finnUtbetalingsgrader(List<DatoIntervallEntitet> gyldigePerioder, Uttaksplan uttaksplan) {
        List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader;
        utbetalingsgrader = uttaksplan.getPerioder()
            .entrySet()
            .stream()
            .filter(it -> gyldigePerioder.stream().anyMatch(p -> p.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(it.getKey().getFom(), it.getKey().getTom()))))
            .flatMap(e -> lagUtbetalingsgrad(e.getKey(), e.getValue()).entrySet().stream())
            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())))
            .entrySet()
            .stream()
            .map(e -> new UtbetalingsgradPrAktivitetDto(e.getKey(), e.getValue()))
            .collect(Collectors.toList());
        return utbetalingsgrader;
    }

    private YtelsespesifiktGrunnlagDto mapTilYtelseSpesifikkType(BehandlingReferanse ref, List<UtbetalingsgradPrAktivitetDto> utbetalingsgrader, Optional<LocalDate> datoForNyeRegler) {
        return switch (ref.getFagsakYtelseType()) {
            case PLEIEPENGER_SYKT_BARN ->
                new PleiepengerSyktBarnGrunnlag(utbetalingsgrader, datoForNyeRegler.orElse(null));
            case PLEIEPENGER_NÆRSTÅENDE ->
                new PleiepengerNærståendeGrunnlag(utbetalingsgrader, datoForNyeRegler.orElse(null));
            case OPPLÆRINGSPENGER -> new OpplæringspengerGrunnlag(utbetalingsgrader);
            default ->
                throw new IllegalStateException("Ikke støttet ytelse for kalkulus Pleiepenger: " + ref.getFagsakYtelseType());
        };
    }

    private static Map<AktivitetDto, PeriodeMedUtbetalingsgradDto> lagUtbetalingsgrad(LukketPeriode periode, UttaksperiodeInfo plan) {
        var perArbeidsforhold = plan.getUtbetalingsgrader()
            .stream()
            .collect(Collectors.toMap(PleiepengerOgOpplæringspengerGrunnlagMapper::mapUtbetalingsgradArbeidsforhold, Function.identity()));

        Map<AktivitetDto, PeriodeMedUtbetalingsgradDto> res = new HashMap<>();
        for (var entry : perArbeidsforhold.entrySet()) {
            var utbetalingsgrader = entry.getValue();
            var aktivitetsgrad = hentAktivitetsgrad(utbetalingsgrader);
            var utbetalingsgradPeriode = lagPeriode(periode, utbetalingsgrader.getUtbetalingsgrad(), aktivitetsgrad);
            res.put(entry.getKey(), utbetalingsgradPeriode);
        }
        return res;
    }

    private static BigDecimal hentAktivitetsgrad(Utbetalingsgrader utbetalingsgrader) {
        if (utbetalingsgrader.getNormalArbeidstid().isZero()) {
            return new BigDecimal(100).subtract(utbetalingsgrader.getUtbetalingsgrad());
        }

        final Duration faktiskArbeidstid;
        if (utbetalingsgrader.getFaktiskArbeidstid() != null) {
            faktiskArbeidstid = utbetalingsgrader.getFaktiskArbeidstid();
        } else {
            faktiskArbeidstid = Duration.ofHours(0L);
        }

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);

        /*
         * XXX: Dette er samme måte å regne ut på som i uttak. På sikt bør vi nok flytte
         *      denne logikken til pleiepenger-barn-uttak.
         */
        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(utbetalingsgrader.getNormalArbeidstid().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
    }


    private static BigDecimal hentAktivitetsgrad(ArbeidsforholdPeriodeInfo arbeidsforholdPeriodeInfo) {
        if (arbeidsforholdPeriodeInfo.getJobberNormalt().isZero()) {
            return BigDecimal.ZERO;
        }

        final Duration faktiskArbeidstid;
        faktiskArbeidstid = arbeidsforholdPeriodeInfo.getJobberNå();

        final BigDecimal HUNDRE_PROSENT = new BigDecimal(100);

        /*
         * XXX: Dette er samme måte å regne ut på som i uttak. På sikt bør vi nok flytte
         *      denne logikken til pleiepenger-barn-uttak.
         */
        final BigDecimal aktivitetsgrad = new BigDecimal(faktiskArbeidstid.toMillis()).setScale(2, RoundingMode.HALF_DOWN)
            .divide(new BigDecimal(arbeidsforholdPeriodeInfo.getJobberNormalt().toMillis()), 2, RoundingMode.HALF_DOWN)
            .multiply(HUNDRE_PROSENT);

        if (aktivitetsgrad.compareTo(HUNDRE_PROSENT) >= 0) {
            return HUNDRE_PROSENT;
        }

        return aktivitetsgrad;
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

    private static PeriodeMedUtbetalingsgradDto lagPeriode(LukketPeriode periode, BigDecimal utbetalingsgrad, BigDecimal aktivitetsgrad) {
        var kalkulusPeriode = new no.nav.folketrygdloven.kalkulus.felles.v1.Periode(periode.getFom(), periode.getTom());
        return new PeriodeMedUtbetalingsgradDto(kalkulusPeriode, utbetalingsgrad, aktivitetsgrad);
    }

    private static AktivitetDto lagArbeidsforhold(Arbeidsforhold arb) {
        return new AktivitetDto(lagAktør(arb),
            arb.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(arb.getArbeidsforholdId()) : null,
            mapUttakArbeidType(arb));
    }

    private static UttakArbeidType mapUttakArbeidType(Arbeidsforhold arb) {
        if (arb.getType().equals(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV_UTEN_ERSTATNING.getKode())) {
            return new UttakArbeidType(no.nav.k9.kodeverk.uttak.UttakArbeidType.IKKE_YRKESAKTIV.getKode());
        }
        return new UttakArbeidType(arb.getType());
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

}
