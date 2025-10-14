package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class AvslagScenarioer {

    /**
     * Avslag pga alder
     */
    public static UngTestScenario avslagAlder(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), Tid.TIDENES_ENDE));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            null,
            null,
            null,
            new LocalDateTimeline<>(p, Utfall.IKKE_OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(40).plusDays(10),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))),
                Collections.emptyList(),
            null);
    }
}
