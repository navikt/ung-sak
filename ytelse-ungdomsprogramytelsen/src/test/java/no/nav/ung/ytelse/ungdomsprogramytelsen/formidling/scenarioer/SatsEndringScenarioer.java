package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SatsEndringScenarioer {

    /**
     * G-regulering
     */
    public static UngTestScenario gRegulering(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, p.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_SATS_REGULERING, DatoIntervallEntitet.fra(p.getFomDato(), p.getTomDato()))),
            Collections.emptyList(), null, null, null, false);
    }

    /**
     * Utvider et eksisterende scenario med en RE_SATS_REGULERING-trigger over hele programperioden.
     *
     * @param scenario - eksisterende scenario som skal utvides
     */
    public static UngTestScenario leggTilGRegulering(UngTestScenario scenario) {
        var fom = scenario.programPerioder().getFirst().getPeriode().getFomDato();
        var tom = scenario.programPerioder().getLast().getPeriode().getTomDato();

        var triggere = new HashSet<>(scenario.behandlingTriggere());
        triggere.add(new Trigger(BehandlingÅrsakType.RE_SATS_REGULERING, DatoIntervallEntitet.fra(fom, tom)));

        return new UngTestScenario(
            scenario.navn(),
            scenario.programPerioder(),
            scenario.satser(),
            scenario.uttakPerioder(),
            scenario.tilkjentYtelsePerioder(),
            scenario.aldersvilkår(),
            scenario.ungdomsprogramvilkår(),
            scenario.fødselsdato(),
            scenario.søknadStartDato(),
            triggere,
            scenario.barn(),
            scenario.dødsdato(),
            scenario.kontrollerInntektPerioder(),
            scenario.periodeMaksDato(),
            scenario.harForlengetPeriode());
    }
}
