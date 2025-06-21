package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.kontrakt.kontroll.BrukKontrollertInntektValg;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektDto;
import no.nav.ung.sak.kontrakt.kontroll.FastsettInntektPeriodeDto;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapportertInntektMapper;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.ytelse.kontroll.ManueltKontrollertInntekt;

import java.math.BigDecimal;
import java.util.Set;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettInntektDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettInntektOppdaterer implements AksjonspunktOppdaterer<FastsettInntektDto> {

    private KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste;
    private RapportertInntektMapper rapportertInntektMapper;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private HistorikkinnslagRepository historikkinnslagRepository;
    ;


    public FastsettInntektOppdaterer() {
        // for CDI
    }

    @Inject
    public FastsettInntektOppdaterer(KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste, RapportertInntektMapper rapportertInntektMapper, ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, HistorikkinnslagRepository historikkinnslagRepository) {
        this.kontrollerteInntektperioderTjeneste = kontrollerteInntektperioderTjeneste;
        this.rapportertInntektMapper = rapportertInntektMapper;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(FastsettInntektDto dto, AksjonspunktOppdaterParameter param) {
        final var sammenslåtteInntekterTidslinje = finnInntektOgKildeTidslinje(dto, param);
        validerVurdertePerioder(param, sammenslåtteInntekterTidslinje);
        kontrollerteInntektperioderTjeneste.opprettKontrollerteInntekterPerioderEtterManuellVurdering(param.getBehandlingId(), sammenslåtteInntekterTidslinje);
        opprettHistorikkinnslag(dto, param.getRef(), sammenslåtteInntekterTidslinje);
        return OppdateringResultat.builder().medTotrinnHvis(true).build();
    }

    private void validerVurdertePerioder(AksjonspunktOppdaterParameter param, LocalDateTimeline<ManueltKontrollertInntekt> sammenslåtteInntekterTidslinje) {
        final var prosesstriggerTidslinje = prosessTriggerPeriodeUtleder.utledTidslinje(param.getBehandlingId());
        final var tidslinjeTilVurdering = prosesstriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var vurdertePerioderSomIkkeSkalVurderes = sammenslåtteInntekterTidslinje.disjoint(tidslinjeTilVurdering);
        if (!vurdertePerioderSomIkkeSkalVurderes.isEmpty()) {
            throw new IllegalStateException("Kan ikke bekrefte perioder som ikke skal vurderes i denne behandlingen: " + vurdertePerioderSomIkkeSkalVurderes);
        }
    }

    private LocalDateTimeline<ManueltKontrollertInntekt> finnInntektOgKildeTidslinje(FastsettInntektDto dto, AksjonspunktOppdaterParameter param) {
        final var brukerOgRegisterTidslinje = rapportertInntektMapper.mapAlleGjeldendeRegisterOgBrukersInntekter(param.getBehandlingId());
        final var saksbehandlerFastsatteInntekterTidslinje = finnSaksbehandlersFastsatteInntekterTidslinje(dto);
        final var inntekterForAlleKilderTidslinje = kombinerInntekterFraAlleKilder(brukerOgRegisterTidslinje, saksbehandlerFastsatteInntekterTidslinje);
        final var valgTidslinje = tidslinjeForValg(dto);
        return finnTidslinjeForInntektOgKilde(valgTidslinje, inntekterForAlleKilderTidslinje);
    }

    private static LocalDateTimeline<ManueltKontrollertInntekt> finnTidslinjeForInntektOgKilde(LocalDateTimeline<ValgOgBegrunnelse> valgTidslinje,
                                                                                               LocalDateTimeline<InntekterPrKilde> inntekterForAlleKilderTidslinje) {
        return valgTidslinje.combine(inntekterForAlleKilderTidslinje, (di, valgOgBegrunnelse, inntekt) -> switch (valgOgBegrunnelse.getValue().valg()) {
            case BRUK_BRUKERS_INNTEKT -> {
                if (inntekt.getValue().brukersRapporterteInntekt().isEmpty()) {
                    throw new IllegalArgumentException("Kan ikke bruke brukers inntekt for periode " + di + " da bruker ikke har rapportert inntekt i perioden");
                }
                yield new LocalDateSegment<>(di, new ManueltKontrollertInntekt(KontrollertInntektKilde.BRUKER, summerRapporterteInntekter(inntekt.getValue().brukersRapporterteInntekt()), valgOgBegrunnelse.getValue().begrunnelse()));
            }
            case BRUK_REGISTER_INNTEKT ->
                new LocalDateSegment<>(di, new ManueltKontrollertInntekt(KontrollertInntektKilde.REGISTER, summerRapporterteInntekter(inntekt.getValue().registersRapporterteInntekt()), valgOgBegrunnelse.getValue().begrunnelse()));
            case MANUELT_FASTSATT ->
                new LocalDateSegment<>(di, new ManueltKontrollertInntekt(KontrollertInntektKilde.SAKSBEHANDLER, inntekt.getValue().saksbehandlersFastsatteInntekt(), valgOgBegrunnelse.getValue().begrunnelse()));
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    private static BigDecimal summerRapporterteInntekter(Set<RapportertInntekt> rapportertInntekts) {
        return rapportertInntekts.stream().map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

    private static LocalDateTimeline<ValgOgBegrunnelse> tidslinjeForValg(FastsettInntektDto dto) {
        return dto.getPerioder().stream().map(
            p -> new LocalDateTimeline<>(p.periode().getFom(), p.periode().getTom(), new ValgOgBegrunnelse(p.valg(), p.begrunnelse()))
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
            .filter(it -> it.fastsattInnntekt() != null)
            .map(p -> new LocalDateTimeline<>(p.periode().getFom(), p.periode().getTom(), BigDecimal.valueOf(p.fastsattInnntekt())))
            .reduce(LocalDateTimeline::crossJoin).orElse(LocalDateTimeline.empty());
    }

    private static LocalDateSegment<InntekterPrKilde> mapTilInntekterPrKilde(LocalDateInterval di, LocalDateSegment<RapporterteInntekter> rapportert, LocalDateSegment<BigDecimal> saksbehandlet) {
        return new LocalDateSegment<>(di, new InntekterPrKilde(
            rapportert.getValue().brukerRapporterteInntekter(),
            rapportert.getValue().registerRapporterteInntekter(), saksbehandlet == null ? null : saksbehandlet.getValue()));
    }

    private void opprettHistorikkinnslag(FastsettInntektDto dto, BehandlingReferanse behandlingReferanse, LocalDateTimeline<ManueltKontrollertInntekt> sammenslåtteInntekterTidslinje) {

        var linjer = dto.getPerioder().stream()
            .map(p -> HistorikkinnslagLinjeBuilder.fraTilEquals("Inntekt for " + finnMåned(p), null, finnValgTekst(sammenslåtteInntekterTidslinje.getSegment(new LocalDateInterval(p.periode().getFom(), p.periode().getTom())))))
            .toList();

        var historikkinnslag = new Historikkinnslag.Builder()
            .medFagsakId(behandlingReferanse.getFagsakId())
            .medBehandlingId(behandlingReferanse.getId())
            .medTittel(SkjermlenkeType.BEREGNING)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medLinjer(linjer)
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private static String finnValgTekst(LocalDateSegment<ManueltKontrollertInntekt> segment) {
        return switch (segment.getValue().kilde()) {
            case BRUKER -> "Rapportert inntekt fra deltaker: " + formater(segment.getValue().samletInntekt().intValue());
            case REGISTER -> "Rapportert inntekt fra a-ordningen: " + formater(segment.getValue().samletInntekt().intValue());
            case SAKSBEHANDLER -> "Manuelt fastsatt beløp: " + formater(segment.getValue().samletInntekt().intValue());
        };
    }

    private static String formater(int i) {
        return String.format("%,d", i).replace(',', ' ');
    }

    private static String finnMåned(FastsettInntektPeriodeDto p) {
        return switch (p.periode().getFom().getMonth()) {
            case JANUARY -> "januar";
            case FEBRUARY -> "februar";
            case MARCH -> "mars";
            case APRIL -> "april";
            case MAY -> "mai";
            case JUNE -> "juni";
            case JULY -> "juli";
            case AUGUST -> "august";
            case SEPTEMBER -> "september";
            case OCTOBER -> "oktober";
            case NOVEMBER -> "november";
            case DECEMBER -> "desember";
        } + " " + p.periode().getFom().getYear();
    }


    record InntekterPrKilde(Set<RapportertInntekt> brukersRapporterteInntekt,
                            Set<RapportertInntekt> registersRapporterteInntekt,
                            BigDecimal saksbehandlersFastsatteInntekt) {
    }

    record ValgOgBegrunnelse(BrukKontrollertInntektValg valg, String begrunnelse) {
    }

}
