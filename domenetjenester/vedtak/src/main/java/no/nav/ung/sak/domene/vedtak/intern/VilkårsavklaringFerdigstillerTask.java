package no.nav.ung.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOppdaterer;

import java.util.List;

@ApplicationScoped
@ProsessTask(VilkårsavklaringFerdigstillerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VilkårsavklaringFerdigstillerTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.vilkårsavklaringFerdigstillerTask";

    private List<VilkårsavklaringOppdaterer> alleVilkårsavklaringOppdaterere;

    public VilkårsavklaringFerdigstillerTask() {
        // for CDI proxy
    }

    @Inject
    public VilkårsavklaringFerdigstillerTask(BehandlingLåsRepository BehandlingLåsRepository,
                                             @Any Instance<VilkårsavklaringOppdaterer> alleVilkårsavklaringOppdaterere) {
        super(BehandlingLåsRepository);
        this.alleVilkårsavklaringOppdaterere = VilkårsavklaringOppdaterer.sortert(alleVilkårsavklaringOppdaterere);
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        long behandlingId = Long.parseLong(prosessTaskData.getBehandlingId());
        alleVilkårsavklaringOppdaterere.forEach(oppdaterer ->
            oppdaterer.settAlleAvklaringerTilFerdig(behandlingId)
        );
    }
}
