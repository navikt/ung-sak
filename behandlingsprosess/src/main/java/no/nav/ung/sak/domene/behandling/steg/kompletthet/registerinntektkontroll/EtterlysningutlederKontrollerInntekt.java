package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.EtterlysningBehov;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.AvvikResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.Avviksvurdering;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.kontroll.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.kontroll.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Utleder behov for opprettelse av etterlysning av uttalelse for kontroll av inntekt.
 */
public class EtterlysningutlederKontrollerInntekt {

    private final BigDecimal akseptertDifferanse;

    public EtterlysningutlederKontrollerInntekt(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    // Denne logikken er utelukkende  avhengig av Etterlysning og trenger ikke uttalelse
    public LocalDateTimeline<EtterlysningBehov> utledBehovForEtterlysninger(
        KontrollerInntektInput input) {
        var relevantTidslinje = input.relevantTidslinje();
        var gjeldendeRapporterteInntekter = input.gjeldendeRapporterteInntekter();
        var gjeldendeEtterlysningTidslinje = input.gjeldendeEtterlysningTidslinje();

        var resultatTidslinje = new LocalDateTimeline<EtterlysningBehov>(List.of());

        // Sjekker om bruker har etterlysning/uttalelse som ikke lenger er gyldig pga endret registeropplysning
        var etterlysningResultatFraEndretRegisteropplysning = finnNyeEtterlysningerGrunnetRegisterendring(gjeldendeRapporterteInntekter, gjeldendeEtterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(etterlysningResultatFraEndretRegisteropplysning, StandardCombinators::coalesceLeftHandSide);

        // Sjekker om bruker har svart på etterlysning og denne fortsatt er gyldig
        var resultatForGodkjenteInntekter = finnTidslinjeForMottatteSvarUtenRegisterendring(gjeldendeRapporterteInntekter, gjeldendeEtterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(resultatForGodkjenteInntekter, StandardCombinators::coalesceLeftHandSide);

        // Sjekker vi må ha etterlysning pga avvik mellom rapportert inntekt og registerinntekt
        var restTidslinjeÅVurdere = relevantTidslinje.disjoint(resultatTidslinje);
        var avviksvurderingMotRegisterinntekt = finnTidslinjeForEtterlysningFraAvvik(gjeldendeRapporterteInntekter, restTidslinjeÅVurdere);
        resultatTidslinje = resultatTidslinje.crossJoin(avviksvurderingMotRegisterinntekt, StandardCombinators::coalesceLeftHandSide);


        resultatTidslinje = resultatTidslinje.crossJoin(relevantTidslinje.mapValue(it -> EtterlysningBehov.INGEN_ETTERLYSNING), StandardCombinators::coalesceLeftHandSide);

        return resultatTidslinje;

    }

    private static LocalDateTimeline<EtterlysningBehov> finnNyeEtterlysningerGrunnetRegisterendring(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                                                    LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                                                    LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        var relevanteEtterlysninger = etterlysningTidslinje.intersection(tidslinjeRelevanteÅrsaker);
        var endringsresultatEtterlysninger = FinnResultatForEndretRegisteropplysninger.finnTidslinjeForEndring(gjeldendeRapporterteInntekter, relevanteEtterlysninger);
        return endringsresultatEtterlysninger.filterValue(it -> it == FinnResultatForEndretRegisteropplysninger.Endringsresultat.ENDRING)
            .mapValue(it -> EtterlysningBehov.ERSTATT_EKSISTERENDE);
    }


    private static LocalDateTimeline<EtterlysningBehov> finnTidslinjeForMottatteSvarUtenRegisterendring(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                                                        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                                                        LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        var mottattSvarTidslinje = etterlysningTidslinje
                .filterValue(it -> it.etterlysning().etterlysningStatus() == EtterlysningStatus.MOTTATT_SVAR).intersection(tidslinjeRelevanteÅrsaker);
        var endringsresultatEtterlysninger = FinnResultatForEndretRegisteropplysninger.finnTidslinjeForEndring(gjeldendeRapporterteInntekter, mottattSvarTidslinje);
        return endringsresultatEtterlysninger.filterValue(it -> it == FinnResultatForEndretRegisteropplysninger.Endringsresultat.INGEN_ENDRING)
                .mapValue(it -> EtterlysningBehov.INGEN_ETTERLYSNING);
    }

    private LocalDateTimeline<EtterlysningBehov> finnTidslinjeForEtterlysningFraAvvik(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        var avvikstidslinje = new Avviksvurdering(akseptertDifferanse).finnAvviksresultatTidslinje(gjeldendeRapporterteInntekter, tidslinjeRelevanteÅrsaker);
        return avvikstidslinje
            .filterValue(it -> it == AvvikResultatType.AVVIK_MED_REGISTERINNTEKT)
            .mapValue(it -> EtterlysningBehov.NY_ETTERLYSNING_DERSOM_INGEN_FINNES); // Vil gi ny frist dersom det ikke eksisterer oppgave
    }


}
