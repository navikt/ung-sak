package no.nav.k9.sak.domene.behandling.steg.iverksettevedtak;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(HenleggBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class HenleggBehandlingTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.henleggBehandling";

    /** Kode fra BehandlingResultatType. */
    public static final String HENLEGGELSE_TYPE_KEY = "henleggesGrunn";

    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;

    HenleggBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public HenleggBehandlingTask(BehandlingRepositoryProvider repositoryProvider,
                                  HenleggBehandlingTjeneste henleggBehandlingTjeneste) {
        super(repositoryProvider.getBehandlingRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.henleggBehandlingTjeneste = henleggBehandlingTjeneste;

    }

    @Override
    protected void doProsesser(ProsessTaskData prosessTaskData, Behandling behandling) {
        var behandlingId = prosessTaskData.getBehandlingId();

        BehandlingResultatType henleggelseType = Optional.ofNullable(prosessTaskData.getPropertyValue(HENLEGGELSE_TYPE_KEY))
            .map(BehandlingResultatType::fraKode)
            .orElse(BehandlingResultatType.MANGLER_BEREGNINGSREGLER);

        henleggBehandlingTjeneste.henleggBehandlingOgAksjonspunkter(behandlingId, henleggelseType, "Forvaltning");
    }
}
