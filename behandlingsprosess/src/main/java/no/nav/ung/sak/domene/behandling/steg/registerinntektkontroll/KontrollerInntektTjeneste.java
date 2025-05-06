package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static no.nav.ung.kodeverk.uttak.Tid.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll.FinnKontrollresultatForIkkeGodkjentUttalelse.finnKontrollresultatForIkkeGodkjentUttalelse;

public class KontrollerInntektTjeneste {

    private final BigDecimal akseptertDifferanse;
    private final Integer rapporteringsfristDagIMåned;

    public KontrollerInntektTjeneste(BigDecimal akseptertDifferanse, Integer rapporteringsfristDagIMåned) {
        this.akseptertDifferanse = akseptertDifferanse;
        this.rapporteringsfristDagIMåned = rapporteringsfristDagIMåned;
    }

    public LocalDateTimeline<Kontrollresultat> utførKontroll(
        LocalDateTimeline<Set<BehandlingÅrsakType>> prosessTriggerTidslinje,
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningTidslinje) {

        var resultatTidslinje = new LocalDateTimeline<Kontrollresultat>(List.of());

        // Sjekker først om vi har relevante årsaker
        final var tidslinjeRelevanteÅrsaker = prosessTriggerTidslinje.filterValue(it -> it.contains(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT));

        final var tidslinjePassertFrist = finnTidslinjePassertFrist();
        final var harIkkePassertRapporteringsfrist = tidslinjeRelevanteÅrsaker.disjoint(tidslinjePassertFrist);

        // Dersom vi ikkje har passert rapporteringsfrist så skal vi vente til rapporteringsfrist
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

    private LocalDateTimeline<Boolean> finnTidslinjePassertFrist() {
        final var sisteDagForPassertFrist = LocalDate.now().getDayOfMonth() <= rapporteringsfristDagIMåned ?
            LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth())
            : LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

        final var tidslinjePassertFrist = new LocalDateTimeline<>(TIDENES_BEGYNNELSE, sisteDagForPassertFrist, true);
        return tidslinjePassertFrist;
    }

}
