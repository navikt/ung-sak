package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingSteg;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.sporing.IngenVerdi;
import no.nav.ung.sak.behandlingslager.behandling.sporing.LagRegelSporing;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlag;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.ung.sak.domene.typer.tid.Virkedager;
import no.nav.ung.sak.ytelse.*;
import no.nav.ung.sak.ytelseperioder.YtelseperiodeUtleder;


@ApplicationScoped
@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef
public class BeregnYtelseSteg implements BehandlingSteg {

    private static final Logger LOGGER = Logger.getLogger(BeregnYtelseSteg.class.getName());

    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private YtelseperiodeUtleder ytelseperiodeUtleder;
    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;

    public BeregnYtelseSteg() {
    }

    @Inject
    public BeregnYtelseSteg(UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
                            TilkjentYtelseRepository tilkjentYtelseRepository,
                            YtelseperiodeUtleder ytelseperiodeUtleder,
                            KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste) {
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.tilkjentYtelseRepository = tilkjentYtelseRepository;
        this.ytelseperiodeUtleder = ytelseperiodeUtleder;
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Henter repository data
        final var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(kontekst.getBehandlingId());
        if (ungdomsytelseGrunnlag.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        final var ytelseTidslinje = ytelseperiodeUtleder.utledYtelsestidslinje(kontekst.getBehandlingId());

        final var kontrollertInntektperiodeTidslinje = kontrollerteInntektperioderTjeneste.hentTidslinje(kontekst.getBehandlingId());

        // Validerer at periodene for rapporterte inntekter er konsistent med ytelsetidslinje
        validerPerioderForRapporterteInntekter(kontrollertInntektperiodeTidslinje, ytelseTidslinje);

        final var satsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje();
        final var totalsatsTidslinje = mapSatserTilTotalbeløpForPerioder(satsTidslinje, ytelseTidslinje);
        final var godkjentUttakTidslinje = finnGodkjentUttakstidslinje(ungdomsytelseGrunnlag.get());

        // Utfør reduksjon og map til tilkjent ytelse
        final var tilkjentYtelseTidslinje = LagTilkjentYtelse.lagTidslinje(godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelInput = lagRegelInput(satsTidslinje, ytelseTidslinje, godkjentUttakTidslinje, totalsatsTidslinje, kontrollertInntektperiodeTidslinje);
        final var regelSporing = lagSporing(tilkjentYtelseTidslinje);
        tilkjentYtelseRepository.lagre(kontekst.getBehandlingId(), tilkjentYtelseTidslinje.mapValue(TilkjentYtelsePeriodeResultat::verdi), regelInput, regelSporing);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static String lagRegelInput(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje, LocalDateTimeline<Boolean> ytelseTidslinje, LocalDateTimeline<Boolean> godkjentUttakTidslinje, LocalDateTimeline<BeregnetSats> totalsatsTidslinje, LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje) {
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

    private static void validerPerioderForRapporterteInntekter(LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje, LocalDateTimeline<Boolean> stønadTidslinje) {
        final var rapporterteInntekterSomIkkeMatcherYtelsesperiode = rapportertInntektTidslinje.stream().filter(s -> harIkkeMatchendeStønadsperiode(s, stønadTidslinje)).toList();
        if (!rapporterteInntekterSomIkkeMatcherYtelsesperiode.isEmpty()) {
            throw new IllegalStateException("Rapportert inntekt har perioder som ikke er dekket av stønadstidslinjen: " + rapporterteInntekterSomIkkeMatcherYtelsesperiode.stream().map(LocalDateSegment::getLocalDateInterval).toList());
        }
    }

    private static boolean harIkkeMatchendeStønadsperiode(LocalDateSegment<Set<RapportertInntekt>> s, LocalDateTimeline<Boolean> stønadTidslinje) {
        return stønadTidslinje.getLocalDateIntervals().stream().noneMatch(intervall -> intervall.equals(s.getLocalDateInterval()));
    }


    public <V> LocalDateTimeline<BeregnetSats> mapSatserTilTotalbeløpForPerioder(
        LocalDateTimeline<UngdomsytelseSatser> satsTidslinje,
        LocalDateTimeline<V> ytelseTidslinje) {
        final var mappetTidslinje = ytelseTidslinje.map(mapTotaltSatsbeløpForSegment(satsTidslinje));
        return mappetTidslinje;
    }

    private <V> Function<LocalDateSegment<V>, List<LocalDateSegment<BeregnetSats>>> mapTotaltSatsbeløpForSegment(LocalDateTimeline<UngdomsytelseSatser> satsTidslinje) {
        return (inntektSegment) -> {
            var delTidslinje = satsTidslinje.intersection(inntektSegment.getLocalDateInterval());
            final BeregnetSats totatSatsbeløpForPeriode = reduser(delTidslinje);
            return List.of(new LocalDateSegment<>(inntektSegment.getFom(), inntektSegment.getTom(), totatSatsbeløpForPeriode));
        };
    }

    private BeregnetSats reduser(LocalDateTimeline<UngdomsytelseSatser> delTidslinje) {
        return delTidslinje.stream().reduce(BeregnetSats.ZERO, this::reduserSegmenterIDelTidslinje, BeregnetSats::adder);
    }

    private BeregnetSats reduserSegmenterIDelTidslinje(BeregnetSats beregnetSats, LocalDateSegment<UngdomsytelseSatser> s2) {
        final var antallVirkedager = Virkedager.beregnAntallVirkedager(s2.getFom(), s2.getTom());
        final var bergnetForSegment = new BeregnetSats(s2.getValue().dagsats(), s2.getValue().dagsatsBarnetillegg()).multipliser(antallVirkedager);
        return beregnetSats.adder(bergnetForSegment);
    }


}
