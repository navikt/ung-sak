package no.nav.ung.sak.domene.vedtak.samordning;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.behandlingslager.task.BehandlingProsessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lagrer vedtaket til en behandling for samordning med andre ytelser.
 * <p>
 * Tilstandsløs: utleder vedtaket kun fra behandlingen og lagrer det uavhengig av hva som finnes fra før.
 * Kan derfor kjøres på nytt, også for eldre behandlinger (se {@link MigrerVedtattYtelseTilSamordningTask}).
 */
@ApplicationScoped
@ProsessTask(LagreVedtattYtelseTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class LagreVedtattYtelseTask extends BehandlingProsessTask {

    public static final String TASKTYPE = "vedtak.lagreVedtattYtelse";
    private static final Logger log = LoggerFactory.getLogger(LagreVedtattYtelseTask.class);

    private BehandlingRepository behandlingRepository;
    private VedtakYtelseUtleder utleder;
    private VedtakYtelseRepository vedtakYtelseRepository;

    LagreVedtattYtelseTask() {
        // for CDI proxy
    }

    @Inject
    public LagreVedtattYtelseTask(BehandlingRepository behandlingRepository,
                                  VedtakYtelseUtleder utleder,
                                  VedtakYtelseRepository vedtakYtelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.utleder = utleder;
        this.vedtakYtelseRepository = vedtakYtelseRepository;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        var behandling = behandlingRepository.hentBehandling(prosessTaskData.getBehandlingId());
        logContext(behandling);

        var vedtakYtelse = utleder.utled(behandling);
        vedtakYtelseRepository.lagre(vedtakYtelse);

        log.info("Lagret vedtak for samordning, innvilget periode={}, antall anvisninger={}",
            vedtakYtelse.harInnvilgetPeriode(), vedtakYtelse.getAnvisninger().size());
    }
}
