package no.nav.ung.ytelse.ungdomsprogramytelsen.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.sporing.IngenVerdi;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.behandling.steg.beregnytelse.BeregnYtelseSteg;
import no.nav.ung.sak.domene.behandling.steg.beregnytelse.LagTilkjentYtelse;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;
import no.nav.ung.sak.ytelse.TilkjentYtelsePeriodeResultat;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Map;


@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class UngBeregnYtelseSteg implements BeregnYtelseSteg {

    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder;

    public UngBeregnYtelseSteg() {
    }

    @Inject
    public UngBeregnYtelseSteg(UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                               TilkjentYtelseRepository tilkjentYtelseRepository,
                               MånedsvisTidslinjeUtleder månedsvisTidslinjeUtleder) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.månedsvisTidslinjeUtleder = månedsvisTidslinjeUtleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Henter repository data
        final var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(kontekst.getBehandlingId());
        if (ungdomsytelseGrunnlag.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        final var månedsvisYtelseTidslinje = månedsvisTidslinjeUtleder.finnMånedsvisPeriodisertePerioder(kontekst.getBehandlingId());

        final var kontrollertInntektperiodeTidslinje = tilkjentYtelseRepository.hentKontrollerInntektTidslinje(kontekst.getBehandlingId());

        // Validerer at periodene for rapporterte inntekter er konsistent med ytelsetidslinje
        validerPerioderForRapporterteInntekter(kontrollertInntektperiodeTidslinje, månedsvisYtelseTidslinje);

        final var satsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje();
        final var totalsatsTidslinje = TilkjentYtelseBeregner.mapSatserTilTotalbeløpForPerioder(satsTidslinje, månedsvisYtelseTidslinje);
        final var godkjentUttakTidslinje = finnGodkjentUttakstidslinje(ungdomsytelseGrunnlag.get());

        // Utfør reduksjon og map til tilkjent ytelse
        final var tilkjentYtelseTidslinje = LagTilkjentYtelse.lagTidslinje(månedsvisYtelseTidslinje, godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelInput = lagRegelInput(satsTidslinje, månedsvisYtelseTidslinje, godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelSporing = lagSporing(tilkjentYtelseTidslinje);
        tilkjentYtelseRepository.lagre(kontekst.getBehandlingId(), tilkjentYtelseTidslinje.mapValue(TilkjentYtelsePeriodeResultat::verdi), regelInput, regelSporing);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static String lagRegelInput(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje, LocalDateTimeline<YearMonth> ytelseTidslinje, LocalDateTimeline<Boolean> godkjentUttakTidslinje, LocalDateTimeline<BeregnetSats> totalsatsTidslinje, LocalDateTimeline<BigDecimal> rapportertInntektTidslinje) {
        final var sporingsMap = Map.of(
            "satsTidslinje", satsTidslinje,
            "ytelseTidslinje", ytelseTidslinje.mapValue(IngenVerdi::ingenVerdi),
            "godkjentUttakTidslinje", godkjentUttakTidslinje.mapValue(IngenVerdi::ingenVerdi),
            "totalsatsTidslinje", totalsatsTidslinje,
            "kontrollertInntektTidslinje", rapportertInntektTidslinje
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

    private static LocalDateTimeline<Boolean> finnGodkjentUttakstidslinje(UngdomsytelseGrunnlag ungdomsytelseGrunnlag) {
        return ungdomsytelseGrunnlag.getUttakPerioder()
            .getPerioder()
            .stream()
            .filter(it -> it.getAvslagsårsak() == null)
            .map(it -> new LocalDateTimeline<>(it.getPeriode().getFomDato(), it.getPeriode().getTomDato(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static void validerPerioderForRapporterteInntekter(LocalDateTimeline<BigDecimal> rapportertInntektTidslinje, LocalDateTimeline<YearMonth> månedstidslinjeForYtelse) {
        final var rapporterteInntekterSomIkkeOverlapperYtelsesperiode = rapportertInntektTidslinje.stream().filter(s -> harIkkeOverlappendeYtelseMåned(s, månedstidslinjeForYtelse)).toList();
        if (!rapporterteInntekterSomIkkeOverlapperYtelsesperiode.isEmpty()) {
            throw new IllegalStateException("Rapportert inntekt har perioder som ikke er dekket av månedstidslinjen: " + rapporterteInntekterSomIkkeOverlapperYtelsesperiode.stream().map(LocalDateSegment::getLocalDateInterval).toList());
        }
    }

    private static boolean harIkkeOverlappendeYtelseMåned(LocalDateSegment<?> s, LocalDateTimeline<YearMonth> månedstidslinje) {
        return månedstidslinje.getLocalDateIntervals().stream().noneMatch(intervall -> intervall.overlaps(s.getLocalDateInterval()));
    }


}
