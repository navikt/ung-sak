package no.nav.k9.sak.web.app.tasks;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(OpprettManuellRevurderingTask.TASKTYPE)
public class OpprettManuellRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettManuellRevurdering";

    private OpprettRevurderingService opprettRevurderingService;


    protected OpprettManuellRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettManuellRevurderingTask(OpprettRevurderingService opprettRevurderingService) {
        this.opprettRevurderingService = opprettRevurderingService;
    }


    @Override
    public void doTask(ProsessTaskData pd) {
        BehandlingÅrsakType behandlingÅrsakType = BehandlingÅrsakType.RE_ANNET;
        BehandlingStegType startStegVedÅpenBehandling = BehandlingStegType.START_STEG;
        var saksnummer = pd.getSaksnummer();
        if (saksnummer == null) {
            final String[] saksnumre = pd.getPayloadAsString().split("\\s+");
            if (saksnumre.length != 1) {
                throw new IllegalStateException("Kan ikke håndtere forespørsel med flere saksnummer grunnet feil i Abakus. Antall: " + saksnumre.length);
            }
            opprettRevurderingService.opprettManuellRevurdering(new Saksnummer(saksnumre[0]), behandlingÅrsakType, startStegVedÅpenBehandling);
        } else {
            opprettRevurderingService.opprettManuellRevurdering(new Saksnummer(saksnummer), behandlingÅrsakType, startStegVedÅpenBehandling);
        }
    }
}
