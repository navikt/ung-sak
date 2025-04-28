package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.finnKontrollresultatForIkkeGodkjentUttalelse;

public class KontrollerInntektTjeneste {

    private final BigDecimal akseptertDifferanse;

    public KontrollerInntektTjeneste(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public LocalDateTimeline<Kontrollresultat> utførKontroll(
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje,
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {


        var resultatTidslinje = new LocalDateTimeline<Kontrollresultat>(List.of());


        // Sjekker først om vi har relevante årsaker
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT) || it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        // Dersom vi ikkje har passert rapporteringsfrist (ikkje har kontroll-årsak) så skal vi vente til rapporteringsfrist
        resultatTidslinje = resultatTidslinje.crossJoin(harIkkePassertRapporteringsfrist.mapValue(it -> Kontrollresultat.utenInntektresultat(KontrollResultatType.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST)));


        final var relevantIkkeGodkjentUttalelse = etterlysningTidslinje.filterValue(it -> it.etterlysning().erBesvartOgIkkeGodkjent()).intersection(tidslinjeRelevanteÅrsaker);
        var kontrollresultatForIkkeGodkjentUttalelse = finnKontrollresultatForIkkeGodkjentUttalelse(gjeldendeRapporterteInntekter, relevantIkkeGodkjentUttalelse).mapValue(Kontrollresultat::utenInntektresultat);
        resultatTidslinje = resultatTidslinje.crossJoin(kontrollresultatForIkkeGodkjentUttalelse, StandardCombinators::coalesceLeftHandSide);

        var avviksvurderingMotRegisterinntekt = new Avviksvurdering(akseptertDifferanse).gjørAvviksvurderingMotRegisterinntekt(
            gjeldendeRapporterteInntekter,
            etterlysningTidslinje,
            tidslinjeRelevanteÅrsaker);

        resultatTidslinje = resultatTidslinje.crossJoin(avviksvurderingMotRegisterinntekt);

        var uhåndertTidslinje = tidslinjeRelevanteÅrsaker.disjoint(resultatTidslinje);
        if (!uhåndertTidslinje.isEmpty()) {
            throw new IllegalStateException("fant perioder som ikke ble håndtert: " + uhåndertTidslinje);
        }
        return resultatTidslinje;

    }

}
