package no.nav.ung.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;

@ApplicationScoped
@ProsessTask(VilkårsavklaringFerdigstillerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VilkårsavklaringFerdigstillerTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.vilkårsavklaringFerdigstillerTask";

    private VilkårsavklaringTjeneste vilkårsavklaringTjeneste;

    public VilkårsavklaringFerdigstillerTask() {
        // for CDI proxy
    }

    @Inject
    public VilkårsavklaringFerdigstillerTask(BehandlingLåsRepository BehandlingLåsRepository,
                                             VilkårsavklaringTjeneste vilkårsavklaringTjeneste) {
        super(BehandlingLåsRepository);
        this.vilkårsavklaringTjeneste = vilkårsavklaringTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        long behandlingId = Long.parseLong(prosessTaskData.getBehandlingId());
        vilkårsavklaringTjeneste.ferdigstillForeslåtteAvklaringer(behandlingId);
    }
}
