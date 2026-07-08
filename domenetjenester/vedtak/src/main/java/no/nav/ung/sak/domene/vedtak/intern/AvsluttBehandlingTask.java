package no.nav.ung.sak.domene.vedtak.intern;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.FagsakProsessTask;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;

@ApplicationScoped
//Bruker lenger enn normal forsinkelse pga abonnement i inntektskomponenten. For tiden får vi ikke avsluttet abonnementet
//før det er synkronisert mot skatt, noe som tar ca 5 min. Kan justeres tilbake til standard forsinkelse når
//inntektskomponenten har justert hos seg, eller om vi venter på synkronisering ved opprettelse av abonnement
@ProsessTask(value = AvsluttBehandlingTask.TASKTYPE, firstDelay = 300, thenDelay = 300)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class AvsluttBehandlingTask extends FagsakProsessTask {

    public static final String TASKTYPE = "iverksetteVedtak.avsluttBehandling";
    private static final Logger log = LoggerFactory.getLogger(AvsluttBehandlingTask.class);
    private AvsluttBehandling tjeneste;

    AvsluttBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public AvsluttBehandlingTask(AvsluttBehandling tjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.tjeneste = tjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandlingId = prosessTaskData.getBehandlingId();
        tjeneste.avsluttBehandling(behandlingId);
        log.info("Utført for behandling: {}", behandlingId);
    }
}
