package no.nav.ung.ytelse.aktivitetspenger.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.sporing.IngenVerdi;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.domene.behandling.steg.beregnytelse.BeregnYtelseSteg;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.InntektsreduksjonKonfigurasjon;
import no.nav.ung.sak.ytelse.LagTilkjentYtelse;
import no.nav.ung.sak.ytelse.TilkjentYtelsePeriodeResultat;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;


@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@BehandlingTypeRef
public class AktivitetspengerBeregnYtelseSteg implements BeregnYtelseSteg {

    public static final BigDecimal REDUKSJONSFAKTOR_ARBEIDSINNTEKT = new BigDecimal("0.66");
    public static final BigDecimal REDUKSJONSFAKTOR_YTELSE = new BigDecimal("0.66");

    private AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;

    public AktivitetspengerBeregnYtelseSteg() {
    }

    @Inject
    public AktivitetspengerBeregnYtelseSteg(AktivitetspengerGrunnlagRepository aktivitetspengerGrunnlagRepository,
                                            TilkjentYtelseRepository tilkjentYtelseRepository,
                                            MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.aktivitetspengerGrunnlagRepository = aktivitetspengerGrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Henter repository data
        final var aktivitetspengerGrunnlag = aktivitetspengerGrunnlagRepository.hentGrunnlag(kontekst.getBehandlingId());
        if (aktivitetspengerGrunnlag.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        final var månedsvisYtelseTidslinje = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(kontekst.getBehandlingId());

        final var kontrollertInntektperiodeTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(kontekst.getBehandlingId());

        // Validerer at periodene for rapporterte inntekter er konsistent med ytelsetidslinje
        validerPerioderForRapporterteInntekter(kontrollertInntektperiodeTidslinje, månedsvisYtelseTidslinje);

        final var aktivSatsTidslinje = aktivitetspengerGrunnlag.get().hentAktivitetspengerSatsTidslinje();
        final var totalsatsTidslinje = TotalBeløpForPeriodeMapper.mapSatserTilTotalbeløpForPerioder(aktivSatsTidslinje, månedsvisYtelseTidslinje);
        final var godkjentUttakTidslinje = totalsatsTidslinje.mapValue(_ -> true);

        // Utfør reduksjon og map til tilkjent ytelse
        final var konfigurasjon = new InntektsreduksjonKonfigurasjon(REDUKSJONSFAKTOR_ARBEIDSINNTEKT, REDUKSJONSFAKTOR_YTELSE);
        final var tilkjentYtelseTidslinje = new LagTilkjentYtelse(konfigurasjon).lagTidslinje(månedsvisYtelseTidslinje, godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelInput = lagRegelInput(aktivSatsTidslinje, månedsvisYtelseTidslinje, godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelSporing = lagSporing(tilkjentYtelseTidslinje);
        tilkjentYtelseRepository.lagre(kontekst.getBehandlingId(), tilkjentYtelseTidslinje.mapValue(TilkjentYtelsePeriodeResultat::verdi), regelInput, regelSporing);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static String lagRegelInput(LocalDateTimeline<AktivitetspengerSatser> satsTidslinje,
                                        LocalDateTimeline<YearMonth> ytelseTidslinje,
                                        LocalDateTimeline<Boolean> godkjentUttakTidslinje,
                                        LocalDateTimeline<BeregnetSats> totalsatsTidslinje,
                                        LocalDateTimeline<KontrollerteInntekter> kontrollertInntektTidslinje) {
        final var sporingsMap = Map.of(
            "satsTidslinje", satsTidslinje,
            "ytelseTidslinje", ytelseTidslinje.mapValue(IngenVerdi::ingenVerdi),
            "godkjentUttakTidslinje", godkjentUttakTidslinje.mapValue(IngenVerdi::ingenVerdi),
            "totalsatsTidslinje", totalsatsTidslinje,
            "kontrollertInntektTidslinje", kontrollertInntektTidslinje
        );
        final var regelInput = LagRegelSporing.lagRegelSporingFraTidslinjer(sporingsMap);
        return regelInput;
    }

    private static String lagSporing(LocalDateTimeline<TilkjentYtelsePeriodeResultat> tilkjentYtelseTidslinje) {
        final var list = tilkjentYtelseTidslinje.toSegments()
            .stream()
            .map(it -> it.getValue().sporing())
            .toList();
        final var sporingMap = Map.of("tilkjentYtelsePerioder", list);
        final var sporing = JsonObjectMapper.toJson(sporingMap, LagRegelSporing.JsonMappingFeil.FACTORY::jsonMappingFeil);
        return sporing;
    }

    private static void validerPerioderForRapporterteInntekter(LocalDateTimeline<KontrollerteInntekter> rapportertInntektTidslinje, LocalDateTimeline<YearMonth> månedstidslinjeForYtelse) {
        final var rapporterteInntekterSomIkkeOverlapperYtelsesperiode = rapportertInntektTidslinje.stream().filter(s -> harIkkeOverlappendeYtelseMåned(s, månedstidslinjeForYtelse)).toList();
        if (!rapporterteInntekterSomIkkeOverlapperYtelsesperiode.isEmpty()) {
            throw new IllegalStateException("Rapportert inntekt har perioder som ikke er dekket av månedstidslinjen: " + rapporterteInntekterSomIkkeOverlapperYtelsesperiode.stream().map(LocalDateSegment::getLocalDateInterval).toList());
        }
    }

    private static boolean harIkkeOverlappendeYtelseMåned(LocalDateSegment<?> s, LocalDateTimeline<YearMonth> månedstidslinje) {
        return månedstidslinje.getLocalDateIntervals().stream().noneMatch(intervall -> intervall.overlaps(s.getLocalDateInterval()));
    }


}
