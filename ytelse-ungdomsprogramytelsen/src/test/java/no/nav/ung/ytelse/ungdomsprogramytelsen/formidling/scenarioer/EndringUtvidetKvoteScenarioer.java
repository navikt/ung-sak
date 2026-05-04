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
import java.util.List;
import java.util.Set;

public class EndringUtvidetKvoteScenarioer {

    /**
     * Scenario der kvoten er utvidet fra opprinnlig sluttdato til ny sluttdato.
     *
     * @param fom               - startdato for programmet
     * @param opprinneligSluttdato - opprinnlig sluttdato (slik det var i forrige behandling)
     * @param nySluttdato       - ny sluttdato etter utvidelse
     */
    public static UngTestScenario utvidetKvote(LocalDate fom, LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        if (!nySluttdato.isAfter(opprinneligSluttdato)) {
            throw new IllegalArgumentException("Ny sluttdato må være etter opprinnelig sluttdato");
        }

        var nyProgramPeriode = new LocalDateInterval(fom, nySluttdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, nySluttdato, BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(fom, nySluttdato)),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(fom),
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_UTVIDET_KVOTE_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(opprinneligSluttdato.plusDays(1), nySluttdato))
            ),
            Collections.emptyList(),
            null, null);
    }
}

