package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.harDiff;

public class Avviksvurdering {

    public static final BigDecimal AKSEPTERT_DIFFERANSE = BigDecimal.valueOf(1000);


    static LocalDateTimeline<KontrollResultat> gjørAvviksvurderingMotRegisterinntekt(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
        LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {

        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        final var inntektDiffKontrollResultat = finnKontrollresultatTidslinje(gjeldendeRapporterteInntekter, tidslinjeRelevanteÅrsaker);

        final var tidslinjeForOppgaveTilBruker = inntektDiffKontrollResultat.filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER));

        // Må finne ut om vi skal sette ny frist hvis registeret har oppdatert seg
        final var oppgaverTilBrukerTidslinje = finnNyOppgaveKontrollresultatTidslinje(gjeldendeRapporterteInntekter, etterlysningTidslinje, tidslinjeForOppgaveTilBruker);

        //Resultat
        return inntektDiffKontrollResultat.crossJoin(oppgaverTilBrukerTidslinje, StandardCombinators::coalesceRightHandSide);
    }

    private static LocalDateTimeline<KontrollResultat> finnNyOppgaveKontrollresultatTidslinje(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysingTidslinje,
        LocalDateTimeline<KontrollResultat> tidslinjeForOppgaveTilBruker) {
        final var oppgaverTilBrukerTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::registerRapporterteInntekter).intersection(tidslinjeForOppgaveTilBruker)
            .combine(etterlysingTidslinje.mapValue(EtterlysningOgRegisterinntekt::registerInntekt),
                (di, gjeldendeRegisterinntekt, registerinntektVedUttalelse) -> {
                    if (registerinntektVedUttalelse == null) {
                        return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
                    }
                    if (!harDiff(registerinntektVedUttalelse.getValue(), gjeldendeRegisterinntekt.getValue())) {
                        return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
                    } else {
                        return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
                    }
                },
                LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return oppgaverTilBrukerTidslinje;
    }

    private static LocalDateTimeline<KontrollResultat> finnKontrollresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {
        final var inntektDiffKontrollResultat = gjeldendeRapporterteInntekter.intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(it ->
            {
                final var register = it.registerRapporterteInntekter().stream()
                    .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                final var bruker = it.brukerRapporterteInntekter().stream()
                    .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

                final var differanse = register.subtract(bruker).abs();

                if (differanse.compareTo(AKSEPTERT_DIFFERANSE) > 0) {
                    return KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER;
                } else {
                    return KontrollResultat.BRUK_INNTEKT_FRA_BRUKER;
                }
            });
        return inntektDiffKontrollResultat;
    }


}
