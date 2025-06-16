package no.nav.ung.sak.web.app.tjenester.behandling.kontroll;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.domene.iay.modell.Inntekt;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.kontrakt.kontroll.*;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.util.*;

public class KontrollerInntektMapper {


    public static KontrollerInntektDto map(List<KontrollertInntektPeriode> kontrollertInntektPerioder,
                                           LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                           List<Inntekt> registerinntekter,
                                           LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger,
                                           LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {

        final var vurdertePerioder = finnVurdertePerioder(
            kontrollertInntektPerioder,
            rapporterteInntekterTidslinje,
            registerinntekter,
            gjeldendeEtterlysninger,
            perioderTilKontroll);
        final var ikkeVurdert = finnIkkeVurdertePerioder(
            kontrollertInntektPerioder,
            rapporterteInntekterTidslinje,
            registerinntekter,
            gjeldendeEtterlysninger,
            perioderTilKontroll);

        final var perioder = new ArrayList<KontrollerInntektPeriodeDto>();
        perioder.addAll(vurdertePerioder);
        perioder.addAll(ikkeVurdert);

        return new KontrollerInntektDto(perioder);
    }

    private static List<KontrollerInntektPeriodeDto> finnVurdertePerioder(List<KontrollertInntektPeriode> kontrollertInntektPerioder, LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje, List<Inntekt> registerinntekter, LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger, LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {
        return kontrollertInntektPerioder.stream()
            .map(it -> mapTilPeriodeDto(
                rapporterteInntekterTidslinje,
                registerinntekter,
                gjeldendeEtterlysninger,
                perioderTilKontroll, it))
            .toList();
    }

    private static List<KontrollerInntektPeriodeDto> finnIkkeVurdertePerioder(
        List<KontrollertInntektPeriode> kontrollertInntektPerioder,
        LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
        List<Inntekt> registerinntekter,
        LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger,
        LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll) {
        final var vurdertePerioderTidslinje = finnTidslinjeForVurdertePerioder(kontrollertInntektPerioder);
        return mapIkkeVurdertePerioder(
            rapporterteInntekterTidslinje,
            registerinntekter,
            gjeldendeEtterlysninger,
            perioderTilKontroll,
            vurdertePerioderTidslinje);
    }

    private static LocalDateTimeline<Boolean> finnTidslinjeForVurdertePerioder(List<KontrollertInntektPeriode> kontrollertInntektPerioder) {
        return kontrollertInntektPerioder.stream().map(KontrollertInntektPeriode::getPeriode)
            .map(it -> new LocalDateTimeline<>(it.toLocalDateInterval(), true))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    private static List<KontrollerInntektPeriodeDto> mapIkkeVurdertePerioder(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                                                             List<Inntekt> registerinntekter,
                                                                             LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger,
                                                                             LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll,
                                                                             LocalDateTimeline<Boolean> vurdertePerioderTidslinje) {
        final var ikkeVurdertTidslinje = perioderTilKontroll.disjoint(vurdertePerioderTidslinje);
        return ikkeVurdertTidslinje.toSegments().stream()
            .map(it -> {
                final var rapporterteInntekter = mapRapporterteInntekter(rapporterteInntekterTidslinje, registerinntekter, it.getLocalDateInterval());
                final var uttalelse = finnUttalelse(gjeldendeEtterlysninger, it.getLocalDateInterval());
                return new KontrollerInntektPeriodeDto(
                    new Periode(it.getFom(), it.getTom()),
                    PeriodeStatus.AVVIK,
                    true,
                    rapporterteInntekter,
                    null,
                    null,
                    null,
                    uttalelse.orElse(null)
                );
            })
            .toList();
    }

    private static KontrollerInntektPeriodeDto mapTilPeriodeDto(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                                                List<Inntekt> registerinntekter,
                                                                LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger,
                                                                LocalDateTimeline<Set<BehandlingÅrsakType>> perioderTilKontroll, KontrollertInntektPeriode it) {
        final var overlappendeRapporterteInntekter = rapporterteInntekterTidslinje.intersection(it.getPeriode().toLocalDateInterval());
        final var rapporterteInntekter = mapRapporterteInntekter(overlappendeRapporterteInntekter, registerinntekter, it.getPeriode().toLocalDateInterval());
        final var uttalelseFraBruker = finnUttalelse(gjeldendeEtterlysninger, it.getPeriode().toLocalDateInterval());

        return new KontrollerInntektPeriodeDto(
            new Periode(it.getPeriode().getFomDato(), it.getPeriode().getTomDato()),
            it.getErManueltVurdert() ? PeriodeStatus.AVVIK : PeriodeStatus.INGEN_AVVIK,
            !perioderTilKontroll.intersection(it.getPeriode().toLocalDateInterval()).isEmpty(),
            rapporterteInntekter,
            it.getInntekt() == null ? null : it.getInntekt().intValue(),
            it.getBegrunnelse(),
            mapTilValg(it),
            uttalelseFraBruker.orElse(null)
        );
    }

    private static Optional<String> finnUttalelse(LocalDateTimeline<EtterlysningData> gjeldendeEtterlysninger, LocalDateInterval periode) {
        final var overlappendeEtterlysning = gjeldendeEtterlysninger.intersection(periode);
        if (overlappendeEtterlysning.isEmpty()) {
            return Optional.empty();
        }
        if (overlappendeEtterlysning.toSegments().size() > 1) {
            throw new IllegalStateException("Forventet å finne maks en overlappende etterlysninger for periode " + periode + ", men fant " + overlappendeEtterlysning.toSegments().size());
        }
        final var uttalelseData = overlappendeEtterlysning.toSegments().first().getValue().uttalelseData();
        return uttalelseData != null ? Optional.ofNullable(uttalelseData.uttalelse()) : Optional.empty();
    }

    private static RapporterteInntekterDto mapRapporterteInntekter(LocalDateTimeline<RapporterteInntekter> rapporterteInntekterTidslinje,
                                                                   List<Inntekt> registerinntekter,
                                                                   LocalDateInterval periode) {
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
        final var inntektsposterFraRegister = finnInntektsposterForPeriode(registerinntekter, periode);

        return new RapporterteInntekterDto(
            new RapportertInntektDto(brukersRapporterteArbeidsinntekt, brukersRapporterteYtelse),
            new RegisterinntektDto(
                new RapportertInntektDto(registersArbeidsinntekt, registersYtelse),
                inntektsposterFraRegister
            )
        );
    }

    private static List<InntektspostFraRegisterDto> finnInntektsposterForPeriode(List<Inntekt> registerinntekter, LocalDateInterval periode) {
        List<InntektspostFraRegisterDto> inntektsposterFraRegister = new ArrayList<>();
        for (Inntekt inntekt : registerinntekter) {
            inntekt.getAlleInntektsposter().stream()
                .filter(ip -> ip.getPeriode().toLocalDateInterval().overlaps(periode))
                .map(ip -> new InntektspostFraRegisterDto(
                    inntekt.getArbeidsgiver() != null ? inntekt.getArbeidsgiver().getIdentifikator() : null,
                    ip.getInntektYtelseType() != null ? ip.getInntektYtelseType().getYtelseType() : null,
                    ip.getBeløp().getVerdi().intValue()))
                .forEach(inntektsposterFraRegister::add);
        }
        return inntektsposterFraRegister;
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
