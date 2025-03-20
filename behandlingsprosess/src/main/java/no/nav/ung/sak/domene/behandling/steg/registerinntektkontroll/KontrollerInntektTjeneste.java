package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.finnKontrollresultatForIkkeGodkjentUttalelse;

import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.uttalelse.Status;
import no.nav.ung.sak.ytelse.BrukersUttalelseForRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

public class KontrollerInntektTjeneste {

    public static LocalDateTimeline<KontrollResultat> utførKontroll(
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje,
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<BrukersUttalelseForRegisterinntekt> uttalelseTidslinje) {


        var resultatTidslinje = new LocalDateTimeline<KontrollResultat>(List.of());


        // Sjekker først om vi har relevante årsaker
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT) || it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        // Dersom vi ikkje har passert rapporteringsfrist (ikkje har kontroll-årsak) så skal vi vente til rapporteringsfrist
        resultatTidslinje = resultatTidslinje.crossJoin(harIkkePassertRapporteringsfrist.mapValue(it -> KontrollResultat.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST));


        final var relevantIkkeGodkjentUttalelse = uttalelseTidslinje.filterValue(it -> it.status().equals(Status.BEKREFTET) && !it.uttalelse().erEndringenGodkjent()).intersection(tidslinjeRelevanteÅrsaker);
        var kontrollresultatForIkkeGodkjentUttalelse = finnKontrollresultatForIkkeGodkjentUttalelse(gjeldendeRapporterteInntekter, relevantIkkeGodkjentUttalelse);
        resultatTidslinje = resultatTidslinje.crossJoin(kontrollresultatForIkkeGodkjentUttalelse, StandardCombinators::coalesceLeftHandSide);

        var avviksvurderingMotRegisterinntekt = Avviksvurdering.gjørAvviksvurderingMotRegisterinntekt(
            gjeldendeRapporterteInntekter,
            uttalelseTidslinje,
            tidslinjeRelevanteÅrsaker);

        resultatTidslinje = resultatTidslinje.crossJoin(avviksvurderingMotRegisterinntekt);

        var uhåndertTidslinje = tidslinjeRelevanteÅrsaker.disjoint(resultatTidslinje);
        if (!uhåndertTidslinje.isEmpty()) {
            throw new IllegalStateException("fant perioder som ikke ble håndtert: " + uhåndertTidslinje);
        }
        return resultatTidslinje;

    }

}
