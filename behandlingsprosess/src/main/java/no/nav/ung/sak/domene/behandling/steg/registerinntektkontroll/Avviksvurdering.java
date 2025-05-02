package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.kontroll.Inntektsresultat;

import java.math.BigDecimal;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.harDiff;

public class Avviksvurdering {


    private final BigDecimal akseptertDifferanse;

    public Avviksvurdering(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    LocalDateTimeline<Kontrollresultat> gjørAvviksvurderingMotRegisterinntekt(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
        LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {

        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        final var kontrollTidslinje = finnKontrollresultatTidslinje(gjeldendeRapporterteInntekter, etterlysningTidslinje, tidslinjeRelevanteÅrsaker);
        final var tidslinjeForOppgaveTilBruker = forOppgaveTilBruker(kontrollTidslinje);

        // Må finne ut om vi skal sette ny frist hvis registeret har oppdatert seg
        final var oppgaverTilBrukerTidslinje = finnNyOppgaveKontrollresultatTidslinje(gjeldendeRapporterteInntekter, etterlysningTidslinje, tidslinjeForOppgaveTilBruker)
            .mapValue(Kontrollresultat::utenInntektresultat);

        //Resultat
        return kontrollTidslinje.crossJoin(oppgaverTilBrukerTidslinje, StandardCombinators::coalesceRightHandSide);
    }

    private LocalDateTimeline<Kontrollresultat> finnKontrollresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                              LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                              LocalDateTimeline<Set<BehandlingÅrsakType>> tidslinjeRelevanteÅrsaker) {
        final var godkjentRegisterinntektTidslinje = etterlysningTidslinje
            .intersection(tidslinjeRelevanteÅrsaker)
            .filterValue(etterlysning -> etterlysning.etterlysning() != null && Boolean.TRUE.equals(etterlysning.etterlysning().erEndringenGodkjent()))
            .mapValue(EtterlysningOgRegisterinntekt::registerInntekt);

        final var brukersRapporteInntekter = gjeldendeRapporterteInntekter
            .intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(RapporterteInntekter::brukerRapporterteInntekter);

        final var brukersGodkjenteEllerRapporterteInntekter = godkjentRegisterinntektTidslinje.crossJoin(brukersRapporteInntekter,
            (di, lhs, rhs) ->
                new LocalDateSegment<>(di, new BrukersAvklarteInntekter(
                    lhs != null ? lhs.getValue() : rhs.getValue(),
                    lhs != null ? BrukersAvklarteInntekterKilde.REGISTER : BrukersAvklarteInntekterKilde.BRUKER))
        );

        final var registerinntektTidslinje = gjeldendeRapporterteInntekter
            .intersection(tidslinjeRelevanteÅrsaker)
            .mapValue(RapporterteInntekter::registerRapporterteInntekter);


        return registerinntektTidslinje.combine(brukersGodkjenteEllerRapporterteInntekter, (di, lhs, rhs) -> {
                if (rhs == null) {
                    return new LocalDateSegment<>(di, Kontrollresultat.utenInntektresultat(KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER));
                }
                return new LocalDateSegment<>(di, finnSamletKontrollresultat(lhs, rhs));
            }, LocalDateTimeline.JoinStyle.CROSS_JOIN)
            .crossJoin(tidslinjeRelevanteÅrsaker.mapValue(it ->
                new Kontrollresultat(
                    KontrollResultatType.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER,
                    Inntektsresultat.ingenInntektFraBruker())));
    }

    private Kontrollresultat finnSamletKontrollresultat(LocalDateSegment<Set<RapportertInntekt>> register, LocalDateSegment<BrukersAvklarteInntekter> godkjentEllerRapportertAvBruker) {
        final var kontrollResultatATFL = finnKontrollresultatForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, register.getValue(), godkjentEllerRapportertAvBruker.getValue().inntekter());
        final var kontrollResultatYtelse = finnKontrollresultatForType(InntektType.YTELSE, register.getValue(), godkjentEllerRapportertAvBruker.getValue().inntekter());

        if (kontrollResultatATFL.equals(KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER) || kontrollResultatYtelse.equals(KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER)) {
            // Prioriterer å opprette oppgave dersom det er avvik på ytelse eller atfl
            return Kontrollresultat.utenInntektresultat(KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER);
        } else if (kontrollResultatATFL.equals(KontrollResultatType.OPPRETT_AKSJONSPUNKT) || kontrollResultatYtelse.equals(KontrollResultatType.OPPRETT_AKSJONSPUNKT)) {
            // Deretter sjekke vi aksjonspunkt
            return Kontrollresultat.utenInntektresultat(KontrollResultatType.OPPRETT_AKSJONSPUNKT);
        } else {
            // Til slutt, bruk godkjent/rapportert fra bruker
            return new Kontrollresultat(KontrollResultatType.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER,
                new Inntektsresultat(
                    godkjentEllerRapportertAvBruker.getValue().inntekter().stream().map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO),
                    godkjentEllerRapportertAvBruker.getValue().kilde().equals(BrukersAvklarteInntekterKilde.BRUKER) ? KontrollertInntektKilde.BRUKER : KontrollertInntektKilde.REGISTER
                )
            );
        }
    }

    private static LocalDateTimeline<Kontrollresultat> forOppgaveTilBruker(LocalDateTimeline<Kontrollresultat> atflInntektDiffKontrollResultat) {
        return atflInntektDiffKontrollResultat
            .filterValue(it -> it.type().equals(KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER));
    }

    private static LocalDateTimeline<KontrollResultatType> finnNyOppgaveKontrollresultatTidslinje(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysingTidslinje,
        LocalDateTimeline<Kontrollresultat> tidslinjeForOppgaveTilBruker) {
        final var oppgaverTilBrukerTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::registerRapporterteInntekter)
            .intersection(tidslinjeForOppgaveTilBruker)
            .combine(etterlysingTidslinje.mapValue(EtterlysningOgRegisterinntekt::registerInntekt),
                (di, gjeldendeRegisterinntekt, registerinntektVedUttalelse) -> {
                    if (registerinntektVedUttalelse == null) {
                        return new LocalDateSegment<>(di, KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER);
                    }
                    if (!harDiff(registerinntektVedUttalelse.getValue(), gjeldendeRegisterinntekt.getValue())) {
                        return new LocalDateSegment<>(di, KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER);
                    } else {
                        return new LocalDateSegment<>(di, KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
                    }
                },
                LocalDateTimeline.JoinStyle.LEFT_JOIN);
        return oppgaverTilBrukerTidslinje;
    }

    private KontrollResultatType finnKontrollresultatForType(InntektType inntektType, Set<RapportertInntekt> registerinntekter, Set<RapportertInntekt> brukersInntekter) {
        final var register = registerinntekter.stream()
            .filter(inntekt -> inntekt.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        final var bruker = brukersInntekter.stream()
            .filter(inntekt -> inntekt.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);

        final var differanse = register.subtract(bruker).abs();

        if (differanse.compareTo(akseptertDifferanse) > 0) {
            if (register.compareTo(BigDecimal.ZERO) == 0) {
                return KontrollResultatType.OPPRETT_AKSJONSPUNKT;
            }
            return KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER;
        } else {
            return KontrollResultatType.BRUK_GODKJENT_ELLER_RAPPORTERT_INNTEKT_FRA_BRUKER;
        }
    }


}
