package no.nav.k9.sak.web.app.tasks;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.k9.sak.typer.Saksnummer;

@ApplicationScoped
@ProsessTask(OpprettAutomatiskRevurderingTask.TASKTYPE)
public class OpprettAutomatiskRevurderingTask implements ProsessTaskHandler {
    public static final String TASKTYPE = "forvaltning.opprettAutomatiskRevurdering";

    private OpprettRevurderingService opprettRevurderingService;

    protected OpprettAutomatiskRevurderingTask() {
        // CDI proxy
    }

    @Inject
    public OpprettAutomatiskRevurderingTask(OpprettRevurderingService opprettRevurderingService) {
        this.opprettRevurderingService = opprettRevurderingService;
    }

    @Override
    public void doTask(ProsessTaskData pd) {
        var saksnummer = Objects.requireNonNull(pd.getSaksnummer(), "saksnummer");
        BehandlingÅrsakType revurderingÅrsak = BehandlingÅrsakType.fraKode(Objects.requireNonNull(pd.getPropertyValue("revurderingÅrsak"), "revurderingÅrsak"));
        BehandlingStegType startStegVedÅpenBehandling = BehandlingStegType.fraKode(Objects.requireNonNull(pd.getPropertyValue("startSteg"), "startSteg"));
        opprettRevurderingService.opprettAutomatiskRevurdering(new Saksnummer(saksnummer), revurderingÅrsak, startStegVedÅpenBehandling);
    }
}
