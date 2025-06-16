package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.kontroll.Inntektsresultat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class KontrollerInntektTjeneste {

    public static final Set<EtterlysningStatus> VENTER_STATUSER = Set.of(EtterlysningStatus.VENTER, EtterlysningStatus.OPPRETTET);
    private BigDecimal akseptertDifferanse;

    public KontrollerInntektTjeneste(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public LocalDateTimeline<Kontrollresultat> utførKontroll(KontrollerInntektInput input) {
        var gjeldendeEtterlysningTidslinje = input.gjeldendeEtterlysningTidslinje();
        if (harEtterlysningerUtenSvar(gjeldendeEtterlysningTidslinje)) {
            throw new IllegalStateException("Alle etterlysninger må enten ha mottatt svar, være avbrutt eller være utløpt før kontroll av inntekter kan utføres.");
        }

        var gjeldendeRapporterteInntekter = input.gjeldendeRapporterteInntekter();
        var relevantTidslinje = input.relevantTidslinje();
        var resultatTidslinje = new LocalDateTimeline<Kontrollresultat>(List.of());

        // Ikke godkjent uttalelse => Aksjonspunkt
        var kontrollresultatForIkkeGodkjentUttalelse = opprettAksjonspunktForIkkeGodkjentUttalelse(gjeldendeEtterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(kontrollresultatForIkkeGodkjentUttalelse, StandardCombinators::coalesceLeftHandSide);

        var avviksresultat = new Avviksvurdering(akseptertDifferanse).finnAvviksresultatTidslinje(gjeldendeRapporterteInntekter, relevantTidslinje);

        // Avvik der registerinntekt = 0 => Aksjonspunkt
        var avvikUtenRegisterinntektResultat = opprettAksjonspunktForAvvikUtenRegisterinntekt(avviksresultat);
        resultatTidslinje = resultatTidslinje.crossJoin(avvikUtenRegisterinntektResultat, StandardCombinators::coalesceLeftHandSide);

        // Inntekt fra bruker = 0 og ingen avvik => Ferdig kontrollert
        var ingenAvvikUtenRapportertInntektFraBruker = lagResultatDersomIngenRapportertInntektFraBrukerOgRegister(avviksresultat, gjeldendeRapporterteInntekter);
        resultatTidslinje = resultatTidslinje.crossJoin(ingenAvvikUtenRapportertInntektFraBruker, StandardCombinators::coalesceLeftHandSide);

        // Bruker inntekt fra bruker eller godkjent inntekt => Ferdig kontrollert
        final var resultatFraGodkjenteInntekter = finnResultatFraGodkjenteInntekter(relevantTidslinje, gjeldendeRapporterteInntekter, gjeldendeEtterlysningTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(resultatFraGodkjenteInntekter, StandardCombinators::coalesceLeftHandSide);


        var uhåndertTidslinje = relevantTidslinje.disjoint(resultatTidslinje);
        if (!uhåndertTidslinje.isEmpty()) {
            throw new IllegalStateException("fant perioder som ikke ble håndtert: " + uhåndertTidslinje);
        }
        return resultatTidslinje;

    }

    private static LocalDateTimeline<Kontrollresultat> opprettAksjonspunktForIkkeGodkjentUttalelse(LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje, LocalDateTimeline<Boolean> relevantTidslinje) {
        final var relevantHarUttalelse = etterlysningTidslinje.filterValue(it -> it.etterlysning().erBesvartOgHarUttalelse()).intersection(relevantTidslinje);
        return relevantHarUttalelse
            .mapValue(it -> Kontrollresultat.utenInntektresultat(KontrollResultatType.OPPRETT_AKSJONSPUNKT));
    }

    private LocalDateTimeline<Kontrollresultat> opprettAksjonspunktForAvvikUtenRegisterinntekt(LocalDateTimeline<AvvikResultatType> avviksresultat) {
        return avviksresultat.filterValue(it -> it == AvvikResultatType.AVVIK_UTEN_REGISTERINNTEKT)
            .mapValue(it -> Kontrollresultat.utenInntektresultat(KontrollResultatType.OPPRETT_AKSJONSPUNKT));
    }

    private LocalDateTimeline<Kontrollresultat> lagResultatDersomIngenRapportertInntektFraBrukerOgRegister(LocalDateTimeline<AvvikResultatType> avviksresultat, LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter) {
        var ingenAvvikTidslinje = avviksresultat.filterValue(it -> it == AvvikResultatType.INGEN_AVVIK);
        // Bruker kilde BRUKER her selv om bruker ikke faktisk har rapportert inntekt
        // Grunnen til dette er at bruker ikke skal rapportere dersom hen ikke har hatt inntekt
        // Hensikten med kilde her er for å kunne se hvilke tilfeller der registerinntekt endrer på utbetalingen
        // Siden vi tillater et visst avvik her er det ikke sikkert at registerinntekten er nøyaktig lik 0
        var inntektFraBrukerTidslinje = gjeldendeRapporterteInntekter.filterValue(it -> !it.brukerRapporterteInntekter().isEmpty());
        return ingenAvvikTidslinje.disjoint(inntektFraBrukerTidslinje)
            .mapValue(it -> new Kontrollresultat(KontrollResultatType.FERDIG_KONTROLLERT, new Inntektsresultat(BigDecimal.ZERO, KontrollertInntektKilde.BRUKER)));
    }


    private static boolean harEtterlysningerUtenSvar(LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {
        return etterlysningTidslinje.toSegments().stream().anyMatch(it -> VENTER_STATUSER.contains(it.getValue().etterlysning().etterlysningStatus()));
    }

    private static LocalDateTimeline<Kontrollresultat> finnResultatFraGodkjenteInntekter(LocalDateTimeline<Boolean> relevantTidslinje, LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {
        final var brukersGodkjenteEllerRapporterteInntekter = sammenstillInntekter(
            relevantTidslinje,
            gjeldendeRapporterteInntekter,
            etterlysningTidslinje);

        return brukersGodkjenteEllerRapporterteInntekter.intersection(relevantTidslinje)
            .mapValue(it -> new Inntektsresultat(summerInntekter(it), it.kilde()))
            .mapValue(it -> new Kontrollresultat(KontrollResultatType.FERDIG_KONTROLLERT, it));
    }

    private static LocalDateTimeline<BrukersAvklarteInntekter> sammenstillInntekter(LocalDateTimeline<Boolean> relevantTidslinje, LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {
        final var godkjentRegisterinntektTidslinje = etterlysningTidslinje
            .intersection(relevantTidslinje)
            .filterValue(etterlysning -> etterlysning.etterlysning() != null && Boolean.TRUE.equals(etterlysning.etterlysning().erBesvartOgHarUttalelse()))
            .mapValue(EtterlysningOgRegisterinntekt::registerInntekt);

        final var brukersRapporteInntekter = gjeldendeRapporterteInntekter
            .intersection(relevantTidslinje)
            .mapValue(RapporterteInntekter::brukerRapporterteInntekter);

        return godkjentRegisterinntektTidslinje.crossJoin(brukersRapporteInntekter,
            (di, lhs, rhs) ->
                new LocalDateSegment<>(di,
                    new BrukersAvklarteInntekter(
                        lhs != null ? lhs.getValue() : rhs.getValue(),
                        lhs != null ? KontrollertInntektKilde.REGISTER : KontrollertInntektKilde.BRUKER))
        );
    }

    private static BigDecimal summerInntekter(BrukersAvklarteInntekter it) {
        return it.inntekter().stream()
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }

}
