package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.kontrakt.kontroll.BrukKontrollertInntektValg;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.*;

import java.math.BigDecimal;
import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettInntektDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettInntektOppdaterer implements AksjonspunktOppdaterer<FastsettInntektDto> {

    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private RapportertInntektMapper rapportertInntektMapper;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;


    public FastsettInntektOppdaterer() {
        // for CDI
    }

    @Inject
    public FastsettInntektOppdaterer(KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, RapportertInntektMapper rapportertInntektMapper, ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
    }

    @Override
    public OppdateringResultat oppdater(FastsettInntektDto dto, AksjonspunktOppdaterParameter param) {
        final var sammenslåtteInntekterTidslinje = finnInntektOgKildeTidslinje(dto, param);
        validerVurdertePerioder(param, sammenslåtteInntekterTidslinje);
        kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderEtterManuellVurdering(param.getBehandlingId(), sammenslåtteInntekterTidslinje);
        return OppdateringResultat.builder().medTotrinnHvis(true).build();
    }

    private void validerVurdertePerioder(AksjonspunktOppdaterParameter param, LocalDateTimeline<RapportertInntektOgKilde> sammenslåtteInntekterTidslinje) {
        final var prosesstriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(param.getBehandlingId());
        final var tidslinjeTilVurdering = prosesstriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var vurdertePerioderSomIkkeSkalVurderes = sammenslåtteInntekterTidslinje.disjoint(tidslinjeTilVurdering);
        if (!vurdertePerioderSomIkkeSkalVurderes.isEmpty()) {
            throw new IllegalStateException("Kan ikke bekrefte perioder som ikke skal vurderes i denne behandlingen: " + vurdertePerioderSomIkkeSkalVurderes);
        }
    }

    private LocalDateTimeline<RapportertInntektOgKilde> finnInntektOgKildeTidslinje(FastsettInntektDto dto, AksjonspunktOppdaterParameter param) {
        final var brukerOgRegisterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(param.getBehandlingId());
        final var saksbehandlerFastsatteInntekterTidslinje = finnSaksbehandlersFastsatteInntekterTidslinje(dto);
        final var inntekterForAlleKilderTidslinje = kombinerInntekterFraAlleKilder(brukerOgRegisterTidslinje, saksbehandlerFastsatteInntekterTidslinje);
        final var valgTidslinje = tidslinjeForValg(dto);
        return finnTidslinjeForInntektOgKilde(valgTidslinje, inntekterForAlleKilderTidslinje);
    }

    private static LocalDateTimeline<RapportertInntektOgKilde> finnTidslinjeForInntektOgKilde(LocalDateTimeline<BrukKontrollertInntektValg> valgTidslinje,
                                                                                              LocalDateTimeline<InntekterPrKilde> inntekterForAlleKilderTidslinje) {
        return valgTidslinje.combine(inntekterForAlleKilderTidslinje, (di, valg, inntekt) -> switch (valg.getValue()) {
            case BRUK_BRUKERS_INNTEKT -> {
                if (inntekt.getValue().brukersRapporterteInntekt().isEmpty()) {
                    throw new IllegalArgumentException("Kan ikke bruke brukers inntekt for periode " + di + " da bruker ikke har rapportert inntekt i perioden");
                }
                yield new LocalDateSegment<>(di, new RapportertInntektOgKilde(KontrollertInntektKilde.BRUKER, summerRapporterteInntekter(inntekt.getValue().brukersRapporterteInntekt())));
            }
            case BRUK_REGISTER_INNTEKT ->
                new LocalDateSegment<>(di, new RapportertInntektOgKilde(KontrollertInntektKilde.REGISTER, summerRapporterteInntekter(inntekt.getValue().registersRapporterteInntekt())));
            case MANUELT_FASTSATT ->
                new LocalDateSegment<>(di, new RapportertInntektOgKilde(KontrollertInntektKilde.SAKSBEHANDLER, inntekt.getValue().saksbehandlersFastsatteInntekt()));
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private static BigDecimal summerRapporterteInntekter(Set<RapportertInntekt> rapportertInntekts) {
        return rapportertInntekts.stream().map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static LocalDateTimeline<BrukKontrollertInntektValg> tidslinjeForValg(FastsettInntektDto dto) {
        return dto.getPerioder().stream().map(
            p -> new LocalDateTimeline<>(p.getPeriode().getFom(), p.getPeriode().getTom(), p.getValg())
        ).reduce(LocalDateTimeline::crossJoin).orElse(LocalDateTimeline.empty());
    }

    private static LocalDateTimeline<InntekterPrKilde> kombinerInntekterFraAlleKilder(LocalDateTimeline<RapporterteInntekter> brukerOgRegisterTidslinje,
                                                                                      LocalDateTimeline<BigDecimal> saksbehandlerFastsatteInntekterTidslinje) {
        return brukerOgRegisterTidslinje.combine(saksbehandlerFastsatteInntekterTidslinje,
            FastsettInntektOppdaterer::mapTilInntekterPrKilde,
            LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private static LocalDateTimeline<BigDecimal> finnSaksbehandlersFastsatteInntekterTidslinje(FastsettInntektDto dto) {
        return dto.getPerioder().stream()
            .filter(it -> it.getFastsattInnntekt() != null)
            .map(p -> new LocalDateTimeline<>(p.getPeriode().getFom(), p.getPeriode().getTom(), BigDecimal.valueOf(p.getFastsattInnntekt())))
            .reduce(LocalDateTimeline::crossJoin).orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegment<InntekterPrKilde> mapTilInntekterPrKilde(LocalDateInterval di, LocalDateSegment<RapporterteInntekter> rapportert, LocalDateSegment<BigDecimal> saksbehandlet) {
        return new LocalDateSegment<>(di, new InntekterPrKilde(
            rapportert.getValue().brukerRapporterteInntekter(),
            rapportert.getValue().registerRapporterteInntekter(), saksbehandlet == null ? null : saksbehandlet.getValue()));
    }


    record InntekterPrKilde(Set<RapportertInntekt> brukersRapporterteInntekt,
                            Set<RapportertInntekt> registersRapporterteInntekt,
                            BigDecimal saksbehandlersFastsatteInntekt) {
    }

}
