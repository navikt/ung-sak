package no.nav.k9.sak.metrikker;

import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import no.nav.vedtak.felles.integrasjon.sensu.SensuEvent;
import no.nav.vedtak.felles.integrasjon.sensu.SensuKlient;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskEvent;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskStatus;

@ApplicationScoped
class TaskStatusEventObserver {

    private static final Set<ProsessTaskStatus> DONE = Set.of(ProsessTaskStatus.KJOERT, ProsessTaskStatus.FERDIG);

    private SensuKlient sensuKlient;

    TaskStatusEventObserver() {
        // for proxy
    }

    @Inject
    TaskStatusEventObserver(SensuKlient sensuKlient) {
        this.sensuKlient = sensuKlient;
    }

    void observerProsessTasks(@Observes ProsessTaskEvent event) {

        // logger bare ferdig her, siden det er tungvindt å telle hver gang på tvers av partisjoner
        if (DONE.contains(event.getNyStatus())) {
            var sensuEvent = SensuEvent.createSensuEvent(
                "antall_ferdig_prosesstask",
                Map.of("prosesstask_type", event.getTaskType()),
                Map.of("antall", 1));

            sensuKlient.logMetrics(sensuEvent);
        }
        // else - andre status telles som totaler ved jevnlig søk i db nå
    }
}
