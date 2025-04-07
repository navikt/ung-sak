package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.kontrakt.kontroll.*;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KontrollerInntektMapper {


    public static KontrollerInntektDto map(List<KontrollertInntektPeriode> kontrollertInntektPerioder,
                                           LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                           LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {

        final var vurdertePerioder = finnVurdertePerioder(kontrollertInntektPerioder, rapporterteInntekterTidslinje, perioderTilKontroll);
        final var ikkeVurdert = finnIkkeVurdertePerioder(kontrollertInntektPerioder, rapporterteInntekterTidslinje, perioderTilKontroll);

        final var perioder = new ArrayList<KontrollerInntektPeriodeDto>();
        perioder.addAll(vurdertePerioder);
        perioder.addAll(ikkeVurdert);

        return new KontrollerInntektDto(perioder);
    }

    private static List<KontrollerInntektPeriodeDto> finnVurdertePerioder(List<KontrollertInntektPeriode> kontrollertInntektPerioder, LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {
        final var vurdertePerioder = kontrollertInntektPerioder.stream()
            .map(it -> mapTilPeriodeDto(rapporterteInntekterTidslinje, perioderTilKontroll, it))
            .toList();
        return vurdertePerioder;
    }

    private static List<KontrollerInntektPeriodeDto> finnIkkeVurdertePerioder(List<KontrollertInntektPeriode> kontrollertInntektPerioder, LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {
        final var vurdertePerioderTidslinje = finnTidslinjeForVurdertePerioder(kontrollertInntektPerioder);
        final var ikkeVurdert = mapIkkeVurdertePerioder(rapporterteInntekterTidslinje, perioderTilKontroll, vurdertePerioderTidslinje);
        return ikkeVurdert;
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeForVurdertePerioder(List<KontrollertInntektPeriode> kontrollertInntektPerioder) {
        return kontrollertInntektPerioder.stream().map(KontrollertInntektPeriode::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.toLocalDateInterval(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static List<KontrollerInntektPeriodeDto> mapIkkeVurdertePerioder(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll, LocalDateTimeline<Boolean> vurdertePerioderTidslinje) {
        final var ikkeVurdertTidslinje = perioderTilKontroll.disjoint(vurdertePerioderTidslinje);
        return ikkeVurdertTidslinje.toSegments().stream()
            .map(it -> {
                final var rapporterteInntekter = mapRapporterteInntekter(rapporterteInntekterTidslinje, it.getLocalDateInterval());
                final var kontrollerInntektPeriodeDto = new KontrollerInntektPeriodeDto(
                    new Periode(it.getFom(), it.getTom()),
                    PeriodeStatus.AVVIK,
                    true,
                    rapporterteInntekter,
                    null,
                    null
                );
                return kontrollerInntektPeriodeDto;
            })
            .toList();
    }

    private static KontrollerInntektPeriodeDto mapTilPeriodeDto(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                                                LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll, KontrollertInntektPeriode it) {
        final var overlappendeRapporterteInntekter = rapporterteInntekterTidslinje.intersection(it.getPeriode().toLocalDateInterval());
        final var rapporterteInntekter = mapRapporterteInntekter(overlappendeRapporterteInntekter, it.getPeriode().toLocalDateInterval());
        return new KontrollerInntektPeriodeDto(
            new Periode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()),
            it.getErManueltVurdert() ? PeriodeStatus.AVVIK : PeriodeStatus.INGEN_AVVIK,
            !perioderTilKontroll.intersection(it.getPeriode().toLocalDateInterval()).isEmpty(),
            rapporterteInntekter,
            it.getInntekt() == null ? null : it.getInntekt().intValue(),
            mapTilValg(it)
        );
    }

    private static RapporterteInntekterDto mapRapporterteInntekter(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje, LocalDateInterval periode) {
        final var overlappendeRapporterteInntekter = rapporterteInntekterTidslinje.intersection(periode);

        if (overlappendeRapporterteInntekter.toSegments().size() > 1) {
            throw new IllegalStateException("Fant flere overlappende rapporterte inntekter for periode " + periode);
        }

        final var brukerRapporterteInntekter = overlappendeRapporterteInntekter.isEmpty() ? new HashSet<RapportertInntekt>() : overlappendeRapporterteInntekter.toSegments().getFirst().getValue().brukerRapporterteInntekter();
        final var registerRapporterteInntekter = overlappendeRapporterteInntekter.isEmpty() ? new HashSet<RapportertInntekt>() : overlappendeRapporterteInntekter.toSegments().getFirst().getValue().registerRapporterteInntekter();

        final var brukersRapporterteArbeidsinntekt = finnRapporterteInntektForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, brukerRapporterteInntekter);
        final var brukersRapporterteYtelse = finnRapporterteInntektForType(InntektType.YTELSE, brukerRapporterteInntekter);
        final var registersArbeidsinntekt = finnRapporterteInntektForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, registerRapporterteInntekter);
        final var registersYtelse = finnRapporterteInntektForType(InntektType.YTELSE, registerRapporterteInntekter);

        return new RapporterteInntekterDto(
            new RapportertInntektDto(brukersRapporterteArbeidsinntekt, brukersRapporterteYtelse),
            new RapportertInntektDto(registersArbeidsinntekt, registersYtelse)
        );
    }

    private static BrukKontrollertInntektValg mapTilValg(KontrollertInntektPeriode it) {
        return switch (it.getKilde()) {
            case BRUKER -> BrukKontrollertInntektValg.BRUK_BRUKERS_INNTEKT;
            case REGISTER -> BrukKontrollertInntektValg.BRUK_REGISTER_INNTEKT;
            case SAKSBEHANDLER -> BrukKontrollertInntektValg.MANUELT_FASTSATT;
        };
    }

    private static Integer finnRapporterteInntektForType(InntektType inntektType, Set<RapportertInntekt> brukerRapporterteInntekter) {
        return brukerRapporterteInntekter.stream()
            .filter(it -> it.inntektType().equals(inntektType))
            .findFirst()
            .map(RapportertInntekt::beløp)
            .map(Number::intValue)
            .orElse(null);
    }

}
