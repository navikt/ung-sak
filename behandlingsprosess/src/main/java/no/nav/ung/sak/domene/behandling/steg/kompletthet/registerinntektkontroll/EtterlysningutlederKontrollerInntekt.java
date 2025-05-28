package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.UtledEtterlysningResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.AvvikResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.Avviksvurdering;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.List;

public class EtterlysningutlederKontrollerInntekt {

    private final BigDecimal akseptertDifferanse;

    public EtterlysningutlederKontrollerInntekt(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public LocalDateTimeline<UtledEtterlysningResultatType> finnEtterlysninger(
        KontrollerInntektInput input) {
        var relevantTidslinje = input.relevantTidslinje();
        var gjeldendeRapporterteInntekter = input.gjeldendeRapporterteInntekter();
        var etterlysningTidslinje = input.etterlysningTidslinje();

        var resultatTidslinje = new LocalDateTimeline<UtledEtterlysningResultatType>(List.of());

        // Sjekker om bruker har etterlysning/uttalelse som ikke lenger er gyldig pga endret registeropplysning
        var etterlysningResultatFraEndretRegisteropplysning = finnNyeEtterlysningerGrunnetRegisterendring(gjeldendeRapporterteInntekter, etterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(etterlysningResultatFraEndretRegisteropplysning, StandardCombinators::coalesceLeftHandSide);

        // Sjekker om bruker har svart på etterlysning og denne fortsatt er gyldig
        var resultatForGodkjenteInntekter = finnTidslinjeForMottatteSvarUtenRegisterendring(gjeldendeRapporterteInntekter, etterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(resultatForGodkjenteInntekter, StandardCombinators::coalesceLeftHandSide);

        // Sjekker vi må ha etterlysning pga avvik mellom rapportert inntekt og registerinntekt
        var restTidslinjeÅVurdere = relevantTidslinje.disjoint(resultatTidslinje);
        var avviksvurderingMotRegisterinntekt = finnTidslinjeForEtterlysningFraAvvik(gjeldendeRapporterteInntekter, restTidslinjeÅVurdere);
        resultatTidslinje = resultatTidslinje.crossJoin(avviksvurderingMotRegisterinntekt, StandardCombinators::coalesceLeftHandSide);


        resultatTidslinje = resultatTidslinje.crossJoin(relevantTidslinje.mapValue(it -> UtledEtterlysningResultatType.INGEN_ETTERLYSNING), StandardCombinators::coalesceLeftHandSide);

        return resultatTidslinje;

    }

    private static LocalDateTimeline<UtledEtterlysningResultatType> finnNyeEtterlysningerGrunnetRegisterendring(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                                                                LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                                                                LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        var etterlysningUtenInnvendinger = etterlysningTidslinje.intersection(tidslinjeRelevanteÅrsaker);
        var endringsresultatEtterlysninger = FinnResultatForEndretRegisteropplysninger.finnTidslinjeForEndring(gjeldendeRapporterteInntekter, etterlysningUtenInnvendinger);
        return endringsresultatEtterlysninger.filterValue(it -> it == FinnResultatForEndretRegisteropplysninger.Endringsresultat.ENDRING)
            .mapValue(it -> UtledEtterlysningResultatType.ERSTATT_EKSISTERENDE);
    }


    private static LocalDateTimeline<UtledEtterlysningResultatType> finnTidslinjeForMottatteSvarUtenRegisterendring(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                                                                    LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                                                                    LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        var godkjentUttalelse = etterlysningTidslinje
                .filterValue(it -> it.etterlysning().etterlysningStatus() == EtterlysningStatus.MOTTATT_SVAR).intersection(tidslinjeRelevanteÅrsaker);
        var endringsresultatEtterlysninger = FinnResultatForEndretRegisteropplysninger.finnTidslinjeForEndring(gjeldendeRapporterteInntekter, godkjentUttalelse);
        return endringsresultatEtterlysninger.filterValue(it -> it == FinnResultatForEndretRegisteropplysninger.Endringsresultat.INGEN_ENDRING)
                .mapValue(it -> UtledEtterlysningResultatType.INGEN_ETTERLYSNING);
    }

    private LocalDateTimeline<UtledEtterlysningResultatType> finnTidslinjeForEtterlysningFraAvvik(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        var avvikstidslinje = new Avviksvurdering(akseptertDifferanse).finnAvviksresultatTidslinje(gjeldendeRapporterteInntekter, tidslinjeRelevanteÅrsaker);
        return avvikstidslinje
            .filterValue(it -> it == AvvikResultatType.AVVIK_MED_REGISTERINNTEKT)
            .mapValue(it -> UtledEtterlysningResultatType.NY_ETTERLYSNING_DERSOM_INGEN_FINNES); // Vil gi ny frist dersom det ikke eksisterer oppgave
    }


}
