package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.harDiff;

public class Avviksvurdering {


    private final BigDecimal akseptertDifferanse;

    public Avviksvurdering(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    LocalDateTimeline<KontrollResultat> gjørAvviksvurderingMotRegisterinntekt(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
        LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {

        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        final var kontrollTidslinje = finnKontrollresultatTidslinje(gjeldendeRapporterteInntekter, etterlysningTidslinje, tidslinjeRelevanteÅrsaker);
        final var tidslinjeForOppgaveTilBruker = forOppgaveTilBruker(kontrollTidslinje);

        // Må finne ut om vi skal sette ny frist hvis registeret har oppdatert seg
        final var oppgaverTilBrukerTidslinje = finnNyOppgaveKontrollresultatTidslinje(gjeldendeRapporterteInntekter, etterlysningTidslinje, tidslinjeForOppgaveTilBruker);

        //Resultat
        return kontrollTidslinje.crossJoin(oppgaverTilBrukerTidslinje, StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<KontrollResultat> finnKontrollresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                              LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                              LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {
        final var godkjentRegisterinntektTidslinje = etterlysningTidslinje
            .intersection(tidslinjeRelevanteÅrsaker)
            .filterValue(etterlysning -> etterlysning.etterlysning() != null && Boolean.TRUE.equals(etterlysning.etterlysning().erEndringenGodkjent()))
            .mapValue(EtterlysningOgRegisterinntekt::registerInntekt);

        final var brukersRapporteInntekter = gjeldendeRapporterteInntekter
            .intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(RapporterteInntekter::brukerRapporterteInntekter);

        final var brukersGodkjenteEllerRapporterteInntekter = godkjentRegisterinntektTidslinje.crossJoin(brukersRapporteInntekter);

        final var registerinntektTidslinje = gjeldendeRapporterteInntekter
            .intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(RapporterteInntekter::registerRapporterteInntekter);


        return registerinntektTidslinje.combine(brukersGodkjenteEllerRapporterteInntekter, (di, lhs, rhs) -> {
                if (rhs == null) {
                    return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER);
                }
                return new LocalDateSegment<>(di, finnSamletKontrollresultat(lhs, rhs));
            }, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .crossJoin(tidslinjeRelevanteÅrsaker.mapValue(it -> KontrollResultat.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER));
    }

    private KontrollResultat finnSamletKontrollresultat(LocalDateSegment<Set<RapportertInntekt>> register, LocalDateSegment<Set<RapportertInntekt>> godkjentEllerRapportertAvBruker) {
        final var kontrollResultatATFL = finnKontrollresultatForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, register.getValue(), godkjentEllerRapportertAvBruker.getValue());
        final var kontrollResultatYtelse = finnKontrollresultatForType(InntektType.YTELSE, register.getValue(), godkjentEllerRapportertAvBruker.getValue());

        if (kontrollResultatATFL.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER) || kontrollResultatYtelse.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER)) {
            // Prioriterer å opprette oppgave dersom det er avvik på ytelse eller atfl
            return KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER;
        } else if (kontrollResultatATFL.equals(KontrollResultat.OPPRETT_AKSJONSPUNKT) || kontrollResultatYtelse.equals(KontrollResultat.OPPRETT_AKSJONSPUNKT)) {
            // Deretter sjekke vi aksjonspunkt
            return KontrollResultat.OPPRETT_AKSJONSPUNKT;
        } else {
            // Til slutt, bruk godkjent/rapportert fra bruker
            return KontrollResultat.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER;
        }
    }

    private static LocalDateTimeline<KontrollResultat> forOppgaveTilBruker(LocalDateTimeline<KontrollResultat> atflInntektDiffKontrollResultat) {
        return atflInntektDiffKontrollResultat
            .filterValue(it -> it.equals(KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER));
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

    private KontrollResultat finnKontrollresultatForType(InntektType inntektType, Set<RapportertInntekt> registerinntekter, Set<RapportertInntekt> brukersInntekter) {
        final var register = registerinntekter.stream()
            .filter(inntekt -> inntekt.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        final var bruker = brukersInntekter.stream()
            .filter(inntekt -> inntekt.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        final var differanse = register.subtract(bruker).abs();

        if (differanse.compareTo(akseptertDifferanse) > 0) {
            if (register.compareTo(BigDecimal.ZERO) == 0) {
                return KontrollResultat.OPPRETT_AKSJONSPUNKT;
            }
            return KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER;
        } else {
            return KontrollResultat.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER;
        }
    }


}
