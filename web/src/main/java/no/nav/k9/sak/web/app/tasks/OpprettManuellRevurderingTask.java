package no.nav.k9.sak.web.app.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(OpprettManuellRevurderingTask.TASKTYPE)
public class OpprettManuellRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettManuellRevurdering";

    private OpprettManuellRevurderingService opprettManuellRevurderingService;

    
    protected OpprettManuellRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettManuellRevurderingTask(OpprettManuellRevurderingService opprettManuellRevurderingService) {
        this.opprettManuellRevurderingService = opprettManuellRevurderingService;
    }

    
    @Override
    public void doTask(ProsessTaskData pd) {
        var saksnummer = pd.getSaksnummer();
        if (saksnummer == null) {
            final String[] saksnumre = pd.getPayloadAsString().split("\\s+");
            if (saksnumre.length != 1) {
                throw new IllegalStateException("Kan ikke håndtere forespørsel med flere saksnummer grunnet feil i Abakus. Antall: " + saksnumre.length);
            }
            opprettManuellRevurderingService.revurder(new Saksnummer(saksnumre[0]));
        } else {
            opprettManuellRevurderingService.revurder(new Saksnummer(saksnummer));
        }
    }
}
