package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.BrukersUttalelseForRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.harDiff;

public class Avviksvurdering {

    public static final BigDecimal AKSEPTERT_DIFFERANSE = BigDecimal.valueOf(1000);


    static Optional<KontrollResultat> gjørAvviksvurderingMotRegisterinntekt(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<BrukersUttalelseForRegisterinntekt> uttalelseTidslinje, LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {
        final var inntektDiffKontrollResultat = finnKontrollresultatTidslinje(gjeldendeRapporterteInntekter, tidslinjeRelevanteÅrsaker);

        final var tidslinjeForOppgaveTilBruker = inntektDiffKontrollResultat.filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER));
        if (!tidslinjeForOppgaveTilBruker.isEmpty()) {

            // Må finne ut om vi skal sette ny frist
            final var oppgaverTilBrukerTidslinje = finnNyOppgaveKontrollresultatTidslinje(gjeldendeRapporterteInntekter, uttalelseTidslinje, tidslinjeForOppgaveTilBruker);

            if (!oppgaverTilBrukerTidslinje.filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST)).isEmpty()) {
                return Optional.of(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
            } else {
                return Optional.of(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
            }

        }

        if (!inntektDiffKontrollResultat.filterValue(it -> it.equals(KontrollResultat.BRUK_INNTEKT_FRA_BRUKER)).isEmpty()) {
            return Optional.of(KontrollResultat.BRUK_INNTEKT_FRA_BRUKER);
        }
        return Optional.empty();
    }

    private static LocalDateTimeline<KontrollResultat> finnNyOppgaveKontrollresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<BrukersUttalelseForRegisterinntekt> uttalelseTidslinje, LocalDateTimeline<KontrollResultat> tidslinjeForOppgaveTilBruker) {
        final var oppgaverTilBrukerTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::getRegisterRapporterteInntekter).intersection(tidslinjeForOppgaveTilBruker)
            .combine(uttalelseTidslinje.mapValue(BrukersUttalelseForRegisterinntekt::registerInntekt), (di, register, uttalelse) -> {
                if (uttalelse == null) {
                    return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
                }
                if (!harDiff(uttalelse.getValue(), register.getValue())) {
                    return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
                } else {
                    return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
                }
            }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return oppgaverTilBrukerTidslinje;
    }

    private static LocalDateTimeline<KontrollResultat> finnKontrollresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {
        final var inntektDiffKontrollResultat = gjeldendeRapporterteInntekter.intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(it ->
            {
                final var register = it.getRegisterRapporterteInntekter().stream()
                    .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
                final var bruker = it.getBrukerRapporterteInntekter().stream()
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
