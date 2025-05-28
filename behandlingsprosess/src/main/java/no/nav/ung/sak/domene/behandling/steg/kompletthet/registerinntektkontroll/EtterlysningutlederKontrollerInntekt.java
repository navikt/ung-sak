package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.sak.domene.behandling.steg.kompletthet.UtledEtterlysningResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.AvvikResultatType;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.Avviksvurdering;
import no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.KontrollerInntektInput;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class EtterlysningutlederKontrollerInntekt {

    private final BigDecimal akseptertDifferanse;

    public EtterlysningutlederKontrollerInntekt(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public LocalDateTimeline<UtledEtterlysningResultatType> finnEtterlysninger(
        KontrollerInntektInput input) {
        var resultatTidslinje = new LocalDateTimeline<UtledEtterlysningResultatType>(List.of());

        var relevantTidslinje = input.relevantTidslinje();
        var gjeldendeRapporterteInntekter = input.gjeldendeRapporterteInntekter();
        var etterlysningTidslinje = input.etterlysningTidslinje();

        // Sjekker først om vi har relevante årsaker

        var etterlysningResultatFraEndretRegisteropplysning = finnNyeEtterlysningerGrunnetRegisterendring(gjeldendeRapporterteInntekter, etterlysningTidslinje, relevantTidslinje);
        resultatTidslinje = resultatTidslinje.crossJoin(etterlysningResultatFraEndretRegisteropplysning, StandardCombinators::coalesceLeftHandSide);

        var restTidslinjeÅVurdere = relevantTidslinje.disjoint(resultatTidslinje);

        var avviksvurderingMotRegisterinntekt = finnTidslinjeForEtterlysningFraAvvik(gjeldendeRapporterteInntekter, restTidslinjeÅVurdere);
        resultatTidslinje = resultatTidslinje.crossJoin(avviksvurderingMotRegisterinntekt, StandardCombinators::coalesceLeftHandSide);
        return resultatTidslinje;

    }

    private static LocalDateTimeline<UtledEtterlysningResultatType> finnNyeEtterlysningerGrunnetRegisterendring(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                                                                LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje,
                                                                                                                LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        // Perioder med uttalelse fra bruker går til aksjonspunkt og trenger ikkje å sjekkes mot diff for gjeldende register
        var etterlysningUtenInnvendinger = etterlysningTidslinje
            .filterValue(it -> !it.etterlysning().erBesvartOgIkkeGodkjent()).intersection(tidslinjeRelevanteÅrsaker);
        var endringsresultatEtterlysninger = FinnResultatForEndretRegisteropplysninger.finnTidslinjeForEndring(gjeldendeRapporterteInntekter, etterlysningUtenInnvendinger);
        return endringsresultatEtterlysninger.filterValue(it -> it.equals(FinnResultatForEndretRegisteropplysninger.Endringsresultat.INGEN_ENDRING))
            .mapValue(it -> UtledEtterlysningResultatType.MED_NY_FRIST);
    }


    private LocalDateTimeline<UtledEtterlysningResultatType> finnTidslinjeForEtterlysningFraAvvik(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<Boolean> tidslinjeRelevanteÅrsaker) {
        //Finner tidslinje der det er avvik mellom register og rapportert inntekt
        var avvikstidslinje = new Avviksvurdering(akseptertDifferanse).finnAvviksresultatTidslinje(gjeldendeRapporterteInntekter, tidslinjeRelevanteÅrsaker);
        return avvikstidslinje
            .filterValue(it -> it.equals(AvvikResultatType.AVVIK_MED_REGISTERINNTEKT))
            .mapValue(it -> UtledEtterlysningResultatType.UTEN_NY_FRIST); // Vil gi ny frist dersom det ikke eksisterer oppgave
    }


}
