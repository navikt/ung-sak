package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.uttalelse.Status;
import no.nav.ung.sak.ytelse.BrukersUttalelseForRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.finnKontrollresultatForIkkeGodkjentUttalelse;

public class KontrollerInntektTjeneste {



    public static KontrollResultat utførKontroll(LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje,
                                          LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                          LocalDateTimeline<BrukersUttalelseForRegisterinntekt> uttalelseTidslinje) {

        // Sjekker først om vi har relevante årsaker
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT) || it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.filterValue(it -> !it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));
        // Dersom vi ikkje har passert rapporteringsfrist (ikkje har kontroll-årsak) så skal vi vente til rapporteringsfrist
        if (!harIkkePassertRapporteringsfrist.isEmpty()) {
            return KontrollResultat.SETT_PÅ_VENT_TIL_RAPPORTERINGSFRIST;
        }

    final var relevantIkkeGodkjentUttalelse = uttalelseTidslinje.filterValue(it -> it.status().equals(Status.BEKREFTET) && !it.uttalelse().erEndringenGodkjent()).intersection(tidslinjeRelevanteÅrsaker);
        if (!relevantIkkeGodkjentUttalelse.isEmpty()) {
            return finnKontrollresultatForIkkeGodkjentUttalelse(gjeldendeRapporterteInntekter, relevantIkkeGodkjentUttalelse);
        }

        final var opprettOppgaveTilBrukerMedNyFrist = Avviksvurdering.gjørAvviksvurderingMotRegisterinntekt(gjeldendeRapporterteInntekter, uttalelseTidslinje, tidslinjeRelevanteÅrsaker);
        return opprettOppgaveTilBrukerMedNyFrist.orElse(KontrollResultat.BRUK_INNTEKT_FRA_BRUKER);

    }

}
