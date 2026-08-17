package no.nav.ung.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårAvklaringOppdaterer;

@ApplicationScoped
@ProsessTask(VilkårsavklaringFerdigstillerTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VilkårsavklaringFerdigstillerTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.vilkårsavklaringFerdigstillerTask";
    Instance<VilkårAvklaringOppdaterer> alleVilkårAvklaringOppdaterere;

    public VilkårsavklaringFerdigstillerTask() {
    }

    public VilkårsavklaringFerdigstillerTask(BehandlingLåsRepository BehandlingLåsRepository,
                                             @Any Instance<VilkårAvklaringOppdaterer> alleVilkårAvklaringOppdaterere) {
        super(BehandlingLåsRepository);
        this.alleVilkårAvklaringOppdaterere = alleVilkårAvklaringOppdaterere;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        alleVilkårAvklaringOppdaterere.stream().forEach(oppdaterer ->
            oppdaterer.settAlleAvklaringerTilFerdig(prosessTaskData.getId())
        );
    }
}
